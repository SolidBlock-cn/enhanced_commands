package pers.solid.ecmd.predicate.entity;

import net.minecraft.command.FloatRangeArgument;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.intellij.lang.annotations.MagicConstant;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.lambda.ToFloatFunction;

import java.util.function.Predicate;

public record RotationPredicateEntry(FloatRangeArgument floatRange, @MagicConstant(stringValues = {"pitch", "yaw"}) String type, ToFloatFunction<Entity> angleFunction, Predicate<Entity> backingPredicate) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    final boolean result = backingPredicate.test(entity);
    final float angle = angleFunction.applyAsFloat(entity);
    return TestResult.of(result, Text.translatable("enhanced_commands.entity_predicate." + type + (result ? ".in_range" : ".out_of_range"), displayName, TextUtil.literal(angle).styled(Styles.ACTUAL), Text.literal(StringUtil.wrapRange(floatRange)).styled(Styles.EXPECTED)));
  }

  @Override
  public String toOptionEntry() {
    return (switch (type) {
      case "pitch" -> "x_rotation";
      case "yaw" -> "y_rotation";
      default -> type;
    }) + "=" + StringUtil.wrapRange(floatRange);
  }
}
