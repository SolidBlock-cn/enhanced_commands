package pers.solid.ecmd.predicate.entity;

import net.minecraft.command.FloatRangeArgument;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.intellij.lang.annotations.MagicConstant;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TextUtil;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public record RotationPredicateEntry(FloatRangeArgument floatRange, @MagicConstant(stringValues = {"pitch", "yaw"}) String type, ToDoubleFunction<Entity> angleFunction, Predicate<Entity> backingPredicate) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    final boolean result = backingPredicate.test(entity);
    final double angle = angleFunction.applyAsDouble(entity);
    return TestResult.of(result, Text.translatable("enhanced_commands.argument.entity_predicate." + type + (result ? ".in_range" : ".out_of_range"), displayName, TextUtil.literal(angle).styled(TextUtil.STYLE_FOR_ACTUAL), Text.literal(StringUtil.wrapRange(floatRange)).styled(TextUtil.STYLE_FOR_EXPECTED)));
  }

  @Override
  public String toOptionEntry() {
    return (switch (type) {
      case "pitch" -> "x_rotation";
      case "yaw" -> "y+rotation";
      default -> type;
    }) + "=" + StringUtil.wrapRange(floatRange);
  }
}
