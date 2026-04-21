package pers.solid.ecmd.entity.predicate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record DistanceEntityPredicate(MinMaxBounds.Doubles distance, PositionOffsetInfo info) implements SpecialEntityPredicate {
  public static final MapCodec<DistanceEntityPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      MinMaxBounds.Doubles.CODEC.fieldOf("distance").forGetter(DistanceEntityPredicate::distance),
      PositionOffsetInfo.CODEC.codec().optionalFieldOf("info", PositionOffsetInfo.NO_OP).forGetter(DistanceEntityPredicate::info)
  ).apply(i, DistanceEntityPredicate::new));
  private static final LoadingCache<PositionOffsetInfo, LoadingCache<ExecutionContext, Vec3>> cache = CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(po -> CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(context -> po.apply(context.positionProvider.getPosition$ec())))));

  @Override
  public boolean test(Entity entity, ExecutionContext context) {
    final Vec3 center = cache.getUnchecked(info).getUnchecked(context);
    return distance.matchesSqr(entity.distanceToSqr(center));
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    final Level expected = context.positionProvider.getWorld$ec();
    final Level actual = entity.level();
    if (!expected.equals(actual)) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.distance.not_local", displayName, TextUtil.literal(actual.dimension().location()), TextUtil.literal(expected.dimension().location())));
    }
    final Vec3 center = cache.getUnchecked(info).getUnchecked(context);
    final double actualDistance = Math.sqrt(entity.distanceToSqr(center));
    if (this.distance.matches(actualDistance)) {
      return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.distance.true", displayName, TextUtil.wrapVector(center), actualDistance, StringUtil.wrapRange(distance)));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.distance.false", displayName, TextUtil.wrapVector(center), actualDistance, StringUtil.wrapRange(distance)));
    }
  }

  @Override
  public EntityPredicateType<DistanceEntityPredicate> getType() {
    return EntityPredicateTypes.DISTANCE;
  }

  @Override
  public String expressAsString() {
    return "[distance=" + StringUtil.wrapRange(distance) + "]";
  }
}
