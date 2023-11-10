package pers.solid.ecmd.predicate.entity;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.mixin.EntitySelectorAccessor;
import pers.solid.ecmd.mixin.EntitySelectorReaderAccessor;

import java.util.ArrayList;
import java.util.List;

/**
 * 此类包含了与实体选择器有关的一些实用方法。
 */
public final class EntitySelectors {
  private EntitySelectors() {
  }

  public static Predicate<Entity> getEntityPredicate(EntitySelector entitySelector, ServerCommandSource source) {
    EntitySelectorExtras.getOf(entitySelector).updateSource(source);
    if (entitySelector.getLimit() < Integer.MAX_VALUE) {
      try {
        final List<? extends Entity> entities = entitySelector.getEntities(source);
        // TODO: 2023年11月10日 check checkSourcePermission
        return Predicates.in(entities);
      } catch (CommandSyntaxException e) {
        return Predicates.alwaysFalse();
      }
    }

    final var accessor = (EntitySelectorAccessor) entitySelector;
    try {
      accessor.callCheckSourcePermission(source);
    } catch (CommandSyntaxException e) {
      return Predicates.alwaysFalse();
    }
    List<Predicate<Entity>> predicates = new ArrayList<>();
    if (!entitySelector.includesNonPlayers()) {
      predicates.add(Entity::isPlayer);
    }
    if (accessor.getPlayerName() != null) {
      predicates.add(entity -> entity instanceof PlayerEntity player && player.getGameProfile().getName().equalsIgnoreCase(accessor.getPlayerName()));
    }
    if (accessor.getUuid() != null) {
      predicates.add(entity -> entity.getUuid().equals(accessor.getUuid()));
    }
    if (entitySelector.isSenderOnly()) {
      predicates.add(entity -> entity.equals(source.getEntity()));
    }
    if (entitySelector.isLocalWorldOnly()) {
      predicates.add(entity -> entity.getWorld().equals(source.getWorld()));
    }

    predicates.add(accessor.callGetPositionPredicate(accessor.getPositionOffset().apply(source.getPosition()))::test);
    return Predicates.and(predicates);
  }

  /**
   * 类似于 {@link EntitySelectorReader#read()}，但是允许省略开头的“@e”等变量。
   */
  public static EntitySelector readOmittibleEntitySelector(@NotNull EntitySelectorReader entitySelectorReader) throws CommandSyntaxException {
    final var accessor = (EntitySelectorReaderAccessor) entitySelectorReader;
    final StringReader stringReader = entitySelectorReader.getReader();

    entitySelectorReader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> {
      suggestionsBuilderConsumer.accept(suggestionsBuilder);
      suggestionsBuilder.suggest("[");
      return suggestionsBuilder.buildFuture();
    });
    if (stringReader.canRead() && stringReader.peek() == '[') {
      stringReader.skip();
      accessor.callReadArguments();
      ((EntitySelectorReaderAccessor) entitySelectorReader).callBuildPredicate();
      return entitySelectorReader.build();
    } else {
      return entitySelectorReader.read();
    }
  }

  public static Predicate<Entity> readOmittibleEntityPredicate(@NotNull EntitySelectorReader entitySelectorReader, ServerCommandSource source) throws CommandSyntaxException {
    return getEntityPredicate(readOmittibleEntitySelector(entitySelectorReader), source);
  }
}
