package pers.solid.ecmd.predicate.entity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record DistanceBlockPredicate(NumberRange.DoubleRange distance, PositionOffsetInfo info) implements SpecialEntityPredicate {
  public static final MapCodec<DistanceBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NumberRange.DoubleRange.CODEC.fieldOf("distance").forGetter(DistanceBlockPredicate::distance),
      PositionOffsetInfo.CODEC.codec().optionalFieldOf("info", PositionOffsetInfo.NO_OP).forGetter(DistanceBlockPredicate::info)
  ).apply(i, DistanceBlockPredicate::new));
  private static final LoadingCache<@NotNull PositionOffsetInfo, LoadingCache<@NotNull ExecutionContext, Vec3d>> cache = CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(po -> CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(context -> po.apply(context.positionProvider.position$ec())))));

  @Override
  public boolean test(@NotNull Entity entity, @NotNull ExecutionContext context) {
    final Vec3d center = cache.getUnchecked(info).getUnchecked(context);
    return distance.testSqrt(entity.squaredDistanceTo(center));
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    final Vec3d center = cache.getUnchecked(info).getUnchecked(context);
    final double actualDistance = Math.sqrt(entity.squaredDistanceTo(center));
    if (this.distance.test(actualDistance)) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.distance.true", displayName, TextUtil.wrapVector(center), actualDistance, StringUtil.wrapRange(distance)));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.distance.false", displayName, TextUtil.wrapVector(center), actualDistance, StringUtil.wrapRange(distance)));
    }
  }

  @Override
  public @NotNull EntityPredicateType<DistanceBlockPredicate> getType() {
    return EntityPredicateTypes.DISTANCE;
  }

  @Override
  public @NotNull String asString() {
    return "distance=" + StringUtil.wrapRange(distance);
  }
}
