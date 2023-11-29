package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TextUtil;

public record AirEntityPredicateEntry(NumberRange.IntRange intRange, boolean inverted) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    final int actualHealth = entity.getAir();
    final MutableText actualHealthText = TextUtil.literal(actualHealth).styled(TextUtil.STYLE_FOR_ACTUAL);
    final MutableText expectedRangeText = Text.literal(StringUtil.wrapRange(intRange)).styled(TextUtil.STYLE_FOR_EXPECTED);
    if (intRange.test(actualHealth)) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.argument.entity_predicate.air.in_range", displayName, actualHealthText, expectedRangeText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.argument.entity_predicate.air.out_of_range", displayName, actualHealthText, expectedRangeText));
    }
  }

  @Override
  public String toOptionEntry() {
    return "air=" + (inverted ? "!" : "") + StringUtil.wrapRange(intRange);
  }
}
