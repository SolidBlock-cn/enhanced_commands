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
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * <p>通过实体选择器实现的实体谓词。在测试时，如果对应的实体选择器没有指定数量，则会根据实体选择器内的一些属性来对实体进行判断，包括判断实体是否为命令的指靠者、实体是否为玩家等。如果实体选择器限制了实体的数量，那么会先选择出这些数量的实体，然后再判断指定的实体是否属于被选择出来的这些实体。
 * <p>实体选择器在创建时，就会直接通过 {@link #asPredicate(EntitySelector, ServerCommandSource)} 计算出具体的、可直接用于判断的谓词。该谓词会在构造函数中直接计算出来，无需手动提供。
 * <p>此对象会包含一个 {@link ServerCommandSource} 对象。
 * <p>在创建了此对象之后，就不要再对 {@link EntitySelector} 进行后续的更改。
 */
public class SelectorEntityPredicate implements EntityPredicate {
  /**
   * 该实体谓词所基于的实体选择器。
   */
  public final EntitySelector entitySelector;
  /**
   * 该实体选择实际用于判断实体的谓词对象。
   */
  public final Predicate<Entity> backingPredicate;
  protected final ServerCommandSource source;

  public SelectorEntityPredicate(EntitySelector entitySelector, ServerCommandSource source) throws CommandSyntaxException {
    this.entitySelector = entitySelector;
    this.backingPredicate = asPredicate(entitySelector, source);
    this.source = source;
  }

  /**
   * 将实体选择器转换为谓词（非 {@link EntityPredicate} 对象。考虑到选择器中会有一些依赖 {@link ServerCommandSource} 的地方，因此需要提供 {@link ServerCommandSource}。
   */
  public static com.google.common.base.Predicate<Entity> asPredicate(EntitySelector entitySelector, ServerCommandSource source) throws CommandSyntaxException {
    EntitySelectorExtras.getOf(entitySelector).updateSource(source);
    final var accessor = (EntitySelectorAccessor) entitySelector;

    if (entitySelector.getLimit() < Integer.MAX_VALUE) {
      final List<? extends Entity> entities = entitySelector.getEntities(source);
      return Predicates.in(entities);
    }
    accessor.callCheckSourcePermission(source);

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
  public TestResult testAndDescribe(Entity entity) throws CommandSyntaxException {
    List<TestResult> descriptions = new ArrayList<>();
    final var accessor = (EntitySelectorAccessor) entitySelector;

    final Text displayName = TextUtil.styled(entity.getDisplayName(), Styles.TARGET);
    if (!entitySelector.includesNonPlayers()) {
      final boolean isPlayer = entity.isPlayer();
      if (isPlayer) {
        descriptions.add(TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.player.true", displayName)));
      } else {
        descriptions.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.player.false", displayName)));
      }
    }
    if (accessor.getPlayerName() != null) {
      if (entity instanceof PlayerEntity player) {
        final boolean matches = player.getGameProfile().getName().equalsIgnoreCase(accessor.getPlayerName());
        descriptions.add(TestResult.of(matches, matches ? Text.translatable("enhanced_commands.entity_predicate.player_name.true", displayName, Text.empty().append(accessor.getPlayerName()).styled(Styles.ACTUAL)) : Text.translatable("enhanced_commands.entity_predicate.player_name.false", displayName, Text.empty().append(player.getGameProfile().getName()).styled(Styles.ACTUAL), Text.literal(accessor.getPlayerName()).styled(Styles.EXPECTED))));
      } else {
        descriptions.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.player_name.not_player", entity.getDisplayName())));
      }
    }
    if (accessor.getUuid() != null) {
      final UUID actual = entity.getUuid();
      final UUID expected = accessor.getUuid();
      if (expected.equals(actual)) {
        descriptions.add(TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.uuid.true", displayName, actual)));
      } else {
        descriptions.add(TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.uuid.false", displayName, TextUtil.styled(Text.literal(actual.toString()), Styles.ACTUAL), TextUtil.styled(Text.literal(expected.toString()), Styles.EXPECTED))));
      }
    }
    if (entitySelector.isSenderOnly()) {
      final Entity realSender = source.getEntity();
      if (entity.equals(realSender)) {
        descriptions.add(TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.sender.true", displayName)));
      } else if (realSender != null) {
        descriptions.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.sender.false", displayName, TextUtil.styled(realSender.getDisplayName(), Styles.EXPECTED))));
      } else {
        descriptions.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.sender.false_without_sender", displayName)));
      }
    }
    if (entitySelector.isLocalWorldOnly()) {
      final World world = entity.getWorld();
      final ServerWorld expectedWorld = source.getWorld();
      if (world.equals(expectedWorld)) {
        descriptions.add(TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.local_world.true", displayName, TextUtil.literal(world.getRegistryKey().getValue()).styled(Styles.ACTUAL))));
      } else {
        descriptions.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.local_world.false", displayName, TextUtil.literal(world.getRegistryKey().getValue()).styled(Styles.ACTUAL), TextUtil.literal(expectedWorld.getRegistryKey().getValue()).styled(Styles.EXPECTED))));

      }
    }

    final Vec3d positionOffset = accessor.getPositionOffset().apply(source.getPosition());

    if (accessor.getBox() != null) {
      final Box box = accessor.getBox().offset(positionOffset);
      final boolean intersects = box.intersects(entity.getBoundingBox());
      descriptions.add(TestResult.of(intersects, Text.translatable("enhanced_commands.entity_predicate.box." + intersects, displayName, Text.literal(String.format("(%s %s %s, %s %s %s)", box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ)).styled(Styles.EXPECTED))));
    }

    final NumberRange.FloatRange distance = accessor.getDistance();
    if (!distance.isDummy()) {
      final String distanceAsString = StringUtil.wrapRange(distance);
      final double squaredDistance = entity.squaredDistanceTo(positionOffset);
      final boolean inRange = distance.testSqrt(squaredDistance);
      descriptions.add(TestResult.of(inRange, Text.translatable("enhanced_commands.entity_predicate.distance." + inRange, displayName, TextUtil.literal(Math.sqrt(squaredDistance)).styled(Styles.ACTUAL), Text.literal(distanceAsString).styled(Styles.EXPECTED))));
    }

    // 以下部分为 EntitySelectorOptions 中的原版部分

    final var predicateDescriptions = EntitySelectorExtras.getOf(entitySelector).predicateDescriptions;
    if (predicateDescriptions != null) {
      for (var predicateDescription : predicateDescriptions) {
        descriptions.add(predicateDescription.apply(source).testAndDescribe(entity, displayName));
      }
    }

    final boolean result = test(entity);
    if (descriptions.isEmpty()) {
      return EntityPredicate.successOrFail(result, entity);
    } else {
      return TestResult.of(result, result ? Text.translatable("enhanced_commands.entity_predicate.pass_selector", displayName) : Text.translatable("enhanced_commands.entity_predicate.fail_selector", displayName), descriptions);
    }
  }
}
