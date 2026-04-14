package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public interface RotationPredicateEntry extends EntityPredicateEntry, StaticEntityPredicate {
  /**
   * 经过 {@link Mth#wrapDegrees} 后处理的最小值，可以大于 max
   */
  float min();

  /**
   * 经过 {@link Mth#wrapDegrees} 后处理的最大值，可以小于 max
   */
  float max();

  float angleOf(Entity entity);

  default String toRangeString() {
    if (min() > max()) {
      return min() - 360f + ".." + max();
    } else {
      return min() + ".." + max();
    }
  }

  @Override
  default boolean test(Entity entity) {
    double actualMin = min();
    double actualMax = max();
    double actualAngle = Mth.wrapDegrees(angleOf(entity));
    if (actualMin > actualMax) {
      return actualAngle >= actualMin || actualAngle <= actualMax;
    } else {
      return actualAngle >= actualMin && actualAngle <= actualMax;
    }
  }

  record Pitch(float min, float max) implements RotationPredicateEntry {
    public Pitch(float min, float max) {
      this.min = Mth.wrapDegrees(min);
      this.max = Mth.wrapDegrees(max);
    }

    public static final MapCodec<Pitch> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.FLOAT.fieldOf("min").forGetter(Pitch::min),
        Codec.FLOAT.fieldOf("max").forGetter(Pitch::max)
    ).apply(i, Pitch::new));

    @Override
    public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
      final float angle = entity.getXRot();
      final MutableComponent actual = TextUtil.literal(angle).withStyle(Styles.ACTUAL);
      final MutableComponent expected = Component.literal(toRangeString()).withStyle(Styles.EXPECTED);
      if (test(entity)) {
        return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.pitch.in_range", displayName, actual, expected));
      } else {
        return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.pitch.out_of_range", displayName, actual, expected));
      }
    }

    @Override
    public float angleOf(Entity entity) {
      return entity.getXRot();
    }

    @Override
    public String toOptionEntry() {
      return "x_rotation=" + toRangeString();
    }

    @Override
    public EntityPredicateType<Pitch> getType() {
      return EntityPredicateTypes.PITCH;
    }
  }

  record Yaw(float min, float max) implements RotationPredicateEntry {
    public Yaw(float min, float max) {
      this.min = Mth.wrapDegrees(min);
      this.max = Mth.wrapDegrees(max);
    }

    public static final MapCodec<Yaw> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.FLOAT.fieldOf("min").forGetter(Yaw::min),
        Codec.FLOAT.fieldOf("max").forGetter(Yaw::max)
    ).apply(i, Yaw::new));

    @Override
    public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
      final float angle = entity.getYRot();
      final MutableComponent actual = TextUtil.literal(angle).withStyle(Styles.ACTUAL);
      final MutableComponent expected = Component.literal(toRangeString()).withStyle(Styles.EXPECTED);
      if (test(entity)) {
        return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.yaw.in_range", displayName, actual, expected));
      } else {
        return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.yaw.out_of_range", displayName, actual, expected));
      }
    }

    @Override
    public float angleOf(Entity entity) {
      return entity.getYRot();
    }

    @Override
    public String toOptionEntry() {
      return "y_rotation=" + toRangeString();
    }

    @Override
    public EntityPredicateType<Yaw> getType() {
      return EntityPredicateTypes.YAW;
    }
  }
}
