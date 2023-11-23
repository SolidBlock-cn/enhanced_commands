package pers.solid.ecmd.predicate.entity;

import com.google.common.base.Predicates;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.mixin.EntitySelectorAccessor;
import pers.solid.ecmd.util.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SelectorEntityPredicate implements EntityPredicate {
  public final EntitySelector entitySelector;
  public final Predicate<Entity> backingPredicate;
  protected final ServerCommandSource source;

  public SelectorEntityPredicate(EntitySelector entitySelector, ServerCommandSource source) {
    this.entitySelector = entitySelector;
    this.backingPredicate = asPredicate(entitySelector, source);
    this.source = source;
  }

  public static com.google.common.base.Predicate<Entity> asPredicate(EntitySelector entitySelector, ServerCommandSource source) {
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
    List<com.google.common.base.Predicate<Entity>> predicates = new ArrayList<>();
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

  @Override
  public boolean test(Entity entity) {
    return backingPredicate.test(entity);
  }

  @Override
  public TestResult testAndDescribe(Entity entity) {
    List<TestResult> descriptions = new ArrayList<>();
    final var accessor = (EntitySelectorAccessor) entitySelector;

    final Text displayName = TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET);
    if (!entitySelector.includesNonPlayers()) {
      final boolean isPlayer = entity.isPlayer();
      descriptions.add(TestResult.of(isPlayer, isPlayer ? Text.translatable("enhanced_commands.argument.entity_predicate.player.true", displayName) : Text.translatable("enhanced_commands.argument.entity_predicate.player.false", displayName)));
    }
    if (accessor.getPlayerName() != null) {
      if (entity instanceof PlayerEntity player) {
        final boolean matches = player.getGameProfile().getName().equalsIgnoreCase(accessor.getPlayerName());
        descriptions.add(TestResult.of(matches, matches ? Text.translatable("enhanced_commands.argument.entity_predicate.player_name.true", displayName, Text.empty().append(accessor.getPlayerName()).styled(TextUtil.STYLE_FOR_ACTUAL)) : Text.translatable("enhanced_commands.argument.entity_predicate.player_name.false", displayName, Text.empty().append(player.getGameProfile().getName()).styled(TextUtil.STYLE_FOR_ACTUAL), Text.literal(accessor.getPlayerName()).styled(TextUtil.STYLE_FOR_EXPECTED))));
      } else {
        descriptions.add(TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.player_name.not_player", entity.getDisplayName())));
      }
    }

    final var predicateDescriptions = EntitySelectorExtras.getOf(entitySelector).predicateDescriptions;
    if (predicateDescriptions != null) {
      for (var predicateDescription : predicateDescriptions) {
        descriptions.add(predicateDescription.apply(source).testAndDescribe(entity));
      }
    }

    final boolean result = test(entity);
    if (descriptions.isEmpty()) {
      return EntityPredicate.successOrFail(result, entity);
    } else {
      return TestResult.of(result, result ? Text.translatable("enhanced_commands.argument.entity_predicate.pass_selector", displayName) : Text.translatable("enhanced_commands.argument.entity_predicate.fail_selector", displayName), descriptions);
    }
  }
}
