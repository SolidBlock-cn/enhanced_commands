package pers.solid.ecmd.entity.predicate;

import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

import java.util.Optional;

public record PoseEntityPredicateEntry(Pose expected, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate, DoesNotRequireValidation {
  public static final ImmutableBiMap<Pose, String> ENTITY_POSE_NAMES = ImmutableBiMap.<Pose, String>builder()
      .put(Pose.STANDING, "standing")
      .put(Pose.FALL_FLYING, "gliding")
      .put(Pose.SLEEPING, "sleeping")
      .put(Pose.SWIMMING, "swimming")
      .put(Pose.SPIN_ATTACK, "spin_attack")
      .put(Pose.CROUCHING, "crouching")
      .put(Pose.LONG_JUMPING, "long_jumping")
      .put(Pose.DYING, "dying")
      .put(Pose.CROAKING, "croaking")
      .put(Pose.USING_TONGUE, "using_tongue")
      .put(Pose.SITTING, "sitting")
      .put(Pose.ROARING, "roaring")
      .put(Pose.SNIFFING, "sniffing")
      .put(Pose.EMERGING, "emerging")
      .put(Pose.DIGGING, "digging")
      .build();
  public static final MapCodec<PoseEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.STRING.flatXmap(s -> Optional.ofNullable(ENTITY_POSE_NAMES.inverse().get(s)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "unknown entity pose name: " + s)), entityPose -> Optional.ofNullable(ENTITY_POSE_NAMES.get(entityPose)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "unknown entity pose"))).fieldOf("pose").forGetter(PoseEntityPredicateEntry::expected),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(PoseEntityPredicateEntry::inverted)
  ).apply(i, PoseEntityPredicateEntry::new));

  @Override
  public boolean test(Entity entity) {
    return (entity.getPose() == expected) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    final Pose actual = entity.getPose();
    final boolean equals = actual == expected;
    if (equals) {
      return TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.pose.true", displayName, Component.translatable(ENTITY_POSE_NAMES.get(actual)).withStyle(Styles.ACTUAL)));
    } else {
      return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.pose.false", displayName, Component.translatable(ENTITY_POSE_NAMES.get(actual)).withStyle(Styles.ACTUAL), Component.translatable(ENTITY_POSE_NAMES.get(expected)).withStyle(Styles.EXPECTED)));
    }
  }

  @Override
  public EntityPredicateType<PoseEntityPredicateEntry> getType() {
    return EntityPredicateTypes.POSE;
  }

  @Override
  public String toOptionEntry() {
    return "pose=" + (inverted ? "!" : "") + ENTITY_POSE_NAMES.get(expected);
  }
}
