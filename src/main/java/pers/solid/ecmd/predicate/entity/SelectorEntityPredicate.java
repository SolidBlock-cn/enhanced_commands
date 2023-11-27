package pers.solid.ecmd.predicate.entity;

import com.google.common.base.Predicates;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.mixin.EntitySelectorAccessor;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
      if (isPlayer) {
        descriptions.add(TestResult.of(true, Text.translatable("enhanced_commands.argument.entity_predicate.player.true", displayName)));
      } else {
        descriptions.add(TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.player.false", displayName)));
      }
    }
    if (accessor.getPlayerName() != null) {
      if (entity instanceof PlayerEntity player) {
        final boolean matches = player.getGameProfile().getName().equalsIgnoreCase(accessor.getPlayerName());
        descriptions.add(TestResult.of(matches, matches ? Text.translatable("enhanced_commands.argument.entity_predicate.player_name.true", displayName, Text.empty().append(accessor.getPlayerName()).styled(TextUtil.STYLE_FOR_ACTUAL)) : Text.translatable("enhanced_commands.argument.entity_predicate.player_name.false", displayName, Text.empty().append(player.getGameProfile().getName()).styled(TextUtil.STYLE_FOR_ACTUAL), Text.literal(accessor.getPlayerName()).styled(TextUtil.STYLE_FOR_EXPECTED))));
      } else {
        descriptions.add(TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.player_name.not_player", entity.getDisplayName())));
      }
    }
    if (accessor.getUuid() != null) {
      final UUID actual = entity.getUuid();
      final UUID expected = accessor.getUuid();
      if (expected.equals(actual)) {
        descriptions.add(TestResult.of(true, Text.translatable("enhanced_commands.argument.entity_predicate.uuid.true", displayName, actual)));
      } else {
        descriptions.add(TestResult.of(true, Text.translatable("enhanced_commands.argument.entity_predicate.uuid.false", displayName, TextUtil.styled(Text.literal(actual.toString()), TextUtil.STYLE_FOR_ACTUAL), TextUtil.styled(Text.literal(expected.toString()), TextUtil.STYLE_FOR_EXPECTED))));
      }
    }
    if (entitySelector.isSenderOnly()) {
      final Entity realSender = source.getEntity();
      if (entity.equals(realSender)) {
        descriptions.add(TestResult.of(true, Text.translatable("enhanced_commands.argument.entity_predicate.sender.true", displayName)));
      } else if (realSender != null) {
        descriptions.add(TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.sender.false", displayName, TextUtil.styled(realSender.getDisplayName(), TextUtil.STYLE_FOR_EXPECTED))));
      } else {
        descriptions.add(TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.sender.false_without_sender", displayName)));
      }
    }
    if (entitySelector.isLocalWorldOnly()) {
      final World world = entity.getWorld();
      final ServerWorld expectedWorld = source.getWorld();
      if (world.equals(expectedWorld)) {
        descriptions.add(TestResult.of(true, Text.translatable("enhanced_commands.argument.entity_predicate.local_world.true", displayName, TextUtil.literal(world.getRegistryKey().getValue()).styled(TextUtil.STYLE_FOR_ACTUAL))));
      } else {
        descriptions.add(TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.local_world.false", displayName, TextUtil.literal(world.getRegistryKey().getValue()).styled(TextUtil.STYLE_FOR_ACTUAL), TextUtil.literal(expectedWorld.getRegistryKey().getValue()).styled(TextUtil.STYLE_FOR_EXPECTED))));

      }
    }

    final Vec3d positionOffset = accessor.getPositionOffset().apply(source.getPosition());

    if (accessor.getBox() != null) {
      final Box box = accessor.getBox().offset(positionOffset);
      final boolean intersects = box.intersects(entity.getBoundingBox());
      descriptions.add(TestResult.of(intersects, Text.translatable("enhanced_commands.argument.entity_predicate.box." + intersects, displayName, Text.literal(String.format("(%s %s %s, %s %s %s)", box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ)).styled(TextUtil.STYLE_FOR_EXPECTED))));
    }

    final NumberRange.FloatRange distance = accessor.getDistance();
    if (!distance.isDummy()) {
      final String distanceAsString = StringUtil.wrapRange(distance);
      final double squaredDistance = entity.squaredDistanceTo(positionOffset);
      final boolean inRange = distance.testSqrt(squaredDistance);
      descriptions.add(TestResult.of(inRange, Text.translatable("enhanced_commands.argument.entity_predicate.distance." + inRange, displayName, TextUtil.literal(Math.sqrt(squaredDistance)).styled(TextUtil.STYLE_FOR_ACTUAL), Text.literal(distanceAsString).styled(TextUtil.STYLE_FOR_EXPECTED))));
    }

    // 以下部分为 EntitySelectorOptions 中的原版部分

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
