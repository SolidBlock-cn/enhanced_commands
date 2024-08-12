package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.BridgeFloatRange;
import pers.solid.ecmd.util.lambda.ToFloatFunction;

import java.util.function.Predicate;

public record RotationPredicateEntry(BridgeFloatRange floatRange, @MagicConstant(stringValues = {"pitch", "yaw"}) String type, ToFloatFunction<Entity> angleFunction, Predicate<Entity> backingPredicate) implements EntityPredicateEntry {
  @Override
  public boolean test(@NotNull Entity entity) {
    return backingPredicate.test(entity);
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    final boolean result = backingPredicate.test(entity);
    final float angle = angleFunction.applyAsFloat(entity);
    final MutableText actual = TextUtil.literal(angle).styled(Styles.ACTUAL);
    final MutableText expected = Text.literal(floatRange.asString()).styled(Styles.EXPECTED);
    if (result) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate." + type + ".in_range", displayName, actual, expected));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate." + type + ".out_of_range", displayName, actual, expected));
    }
  }

  @Override
  public String toOptionEntry() {
    return (switch (type) {
      case "pitch" -> "x_rotation";
      case "yaw" -> "y_rotation";
      default -> type;
    }) + "=" + floatRange.asString();
  }
}
