package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TextUtil;

public record FireEntityPredicateEntry(NumberRange.IntRange intRange, boolean inverted) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    final int actual = entity.getFireTicks();
    final MutableText actualValueText = TextUtil.literal(actual).styled(TextUtil.STYLE_FOR_ACTUAL);
    final MutableText expectedRangeText = Text.literal(StringUtil.wrapRange(intRange)).styled(TextUtil.STYLE_FOR_EXPECTED);
    if (intRange.test(actual)) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.argument.entity_predicate.fire.in_range", displayName, actualValueText, expectedRangeText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.argument.entity_predicate.fire.out_of_range", displayName, actualValueText, expectedRangeText));
    }
  }

  @Override
  public String toOptionEntry() {
    return "fire=" + (inverted ? "!" : "") + StringUtil.wrapRange(intRange);
  }
}
