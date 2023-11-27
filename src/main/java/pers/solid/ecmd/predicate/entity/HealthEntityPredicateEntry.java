package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TextUtil;

public record HealthEntityPredicateEntry(NumberRange.FloatRange floatRange, boolean inverted) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    if (!(entity instanceof LivingEntity livingEntity)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.commands.health.get.single.not_living", displayName));
    } else {
      final float actualHealth = livingEntity.getHealth();
      final MutableText actualHealthText = TextUtil.literal(actualHealth).styled(TextUtil.STYLE_FOR_ACTUAL);
      final MutableText expectedRangeText = Text.literal(StringUtil.wrapRange(floatRange)).styled(TextUtil.STYLE_FOR_EXPECTED);
      if (floatRange.test(actualHealth)) {
        return TestResult.of(!inverted, Text.translatable("enhanced_commands.argument.entity_predicate.health.in_range", displayName, actualHealthText, expectedRangeText));
      } else {
        return TestResult.of(inverted, Text.translatable("enhanced_commands.argument.entity_predicate.health.out_of_range", displayName, actualHealthText, expectedRangeText));
      }
    }
  }

  @Override
  public String toOptionEntry() {
    return "health=" + (inverted ? "!" : "") + StringUtil.wrapRange(floatRange);
  }
}
