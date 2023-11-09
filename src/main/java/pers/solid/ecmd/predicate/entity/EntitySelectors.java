package pers.solid.ecmd.predicate.entity;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.mixin.EntitySelectorAccessor;

import java.util.ArrayList;
import java.util.List;

/**
 * 此类包含了与实体选择器有关的一些实用方法。
 */
public final class EntitySelectors {
  private EntitySelectors() {
  }

  public static Predicate<Entity> getEntityPredicate(EntitySelector entitySelector, ServerCommandSource source) throws CommandSyntaxException {
    EntitySelectorExtras.getOf(entitySelector).updateSource(source);
    if (entitySelector.getLimit() < Integer.MAX_VALUE) {
      final List<? extends Entity> entities = entitySelector.getEntities(source);
      return Predicates.in(entities);
    }

    final var accessor = (EntitySelectorAccessor) entitySelector;
    accessor.callCheckSourcePermission(source);
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
}
