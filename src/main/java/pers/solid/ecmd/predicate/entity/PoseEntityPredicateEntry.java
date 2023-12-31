package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableBiMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.Styles;

public record PoseEntityPredicateEntry(@NotNull EntityPose expected, boolean inverted) implements EntityPredicateEntry {
  public static final ImmutableBiMap<EntityPose, String> ENTITY_POSE_NAMES = ImmutableBiMap.<EntityPose, String>builder()
      .put(EntityPose.STANDING, "standing")
      .put(EntityPose.FALL_FLYING, "fall_flying")
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

  @Override
  public boolean test(Entity entity) {
    return (entity.getPose() == expected) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    final EntityPose actual = entity.getPose();
    final boolean equals = actual == expected;
    if (equals) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.pose.true", displayName, Text.translatable(ENTITY_POSE_NAMES.get(actual)).styled(Styles.ACTUAL)));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.pose.false", displayName, Text.translatable(ENTITY_POSE_NAMES.get(actual)).styled(Styles.ACTUAL), Text.translatable(ENTITY_POSE_NAMES.get(expected)).styled(Styles.EXPECTED)));
    }
  }

  @Override
  public String toOptionEntry() {
    return "pose=" + (inverted ? "!" : "") + ENTITY_POSE_NAMES.get(expected);
  }
}
