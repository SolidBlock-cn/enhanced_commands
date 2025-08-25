package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableBiMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;

import java.util.Optional;

public record PoseEntityPredicateEntry(@NotNull EntityPose expected, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final ImmutableBiMap<EntityPose, String> ENTITY_POSE_NAMES = ImmutableBiMap.<EntityPose, String>builder()
      .put(EntityPose.STANDING, "standing")
      .put(EntityPose.GLIDING, "gliding")
      .put(EntityPose.SLEEPING, "sleeping")
      .put(EntityPose.SWIMMING, "swimming")
      .put(EntityPose.SPIN_ATTACK, "spin_attack")
      .put(EntityPose.CROUCHING, "crouching")
      .put(EntityPose.LONG_JUMPING, "long_jumping")
      .put(EntityPose.DYING, "dying")
      .put(EntityPose.CROAKING, "croaking")
      .put(EntityPose.USING_TONGUE, "using_tongue")
      .put(EntityPose.SITTING, "sitting")
      .put(EntityPose.ROARING, "roaring")
      .put(EntityPose.SNIFFING, "sniffing")
      .put(EntityPose.EMERGING, "emerging")
      .put(EntityPose.DIGGING, "digging")
      .build();
  public static final MapCodec<PoseEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.STRING.flatXmap(s -> Optional.ofNullable(ENTITY_POSE_NAMES.inverse().get(s)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "unknown entity pose name: " + s)), entityPose -> Optional.ofNullable(ENTITY_POSE_NAMES.get(entityPose)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "unknown entity pose"))).fieldOf("pose").forGetter(PoseEntityPredicateEntry::expected),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(PoseEntityPredicateEntry::inverted)
  ).apply(i, PoseEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    return (entity.getPose() == expected) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    final EntityPose actual = entity.getPose();
    final boolean equals = actual == expected;
    if (equals) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.pose.true", displayName, Text.translatable(ENTITY_POSE_NAMES.get(actual)).styled(Styles.ACTUAL)));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.pose.false", displayName, Text.translatable(ENTITY_POSE_NAMES.get(actual)).styled(Styles.ACTUAL), Text.translatable(ENTITY_POSE_NAMES.get(expected)).styled(Styles.EXPECTED)));
    }
  }

  @Override
  public @NotNull EntityPredicateType<PoseEntityPredicateEntry> getType() {
    return EntityPredicateTypes.POSE;
  }

  @Override
  public String toOptionEntry() {
    return "pose=" + (inverted ? "!" : "") + ENTITY_POSE_NAMES.get(expected);
  }
}
