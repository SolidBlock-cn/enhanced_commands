package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record DistanceEntityPredicate(@NotNull NumberRange.DoubleRange distance, @NotNull Vec3d pos) implements SpecialEntityPredicate {
  @Override
  public boolean test(@NotNull Entity entity) {
    return distance.testSqrt(entity.squaredDistanceTo(pos));
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    final double squaredDistance = entity.squaredDistanceTo(pos);
    final Text actualText = TextUtil.literal(Math.sqrt(squaredDistance)).styled(Styles.ACTUAL);
    final Text expectedText = Text.literal(StringUtil.wrapRange(distance)).styled(Styles.EXPECTED);
    if (distance.testSqrt(squaredDistance)) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.distance.true", displayName, actualText, expectedText));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.distance.false", displayName, actualText, expectedText));
    }
  }
}
