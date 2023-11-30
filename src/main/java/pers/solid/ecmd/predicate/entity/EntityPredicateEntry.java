package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.FloatRangeArgument;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.ApiStatus;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TextUtil;

public interface EntityPredicateEntry {
  @ApiStatus.NonExtendable
  default TestResult testAndDescribe(Entity entity) throws CommandSyntaxException {
    return testAndDescribe(entity, TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET));
  }

  TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException;

  String toOptionEntry();

  static <E extends Entity> TestResult testInt(E entity, int actual, NumberRange.IntRange expected, Text criterionName, Text entityName, boolean inverted) {
    final MutableText actualText = TextUtil.literal(actual).styled(TextUtil.STYLE_FOR_ACTUAL);
    final MutableText expectedText = Text.literal(StringUtil.wrapRange(expected)).styled(TextUtil.STYLE_FOR_EXPECTED);
    if (expected.test(actual)) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.argument.entity_predicate.general.in_range", criterionName, entityName, actualText, expectedText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.argument.entity_predicate.general.out_of_range", criterionName, entityName, actualText, expectedText));
    }
  }

  static <E extends Entity> TestResult testFloat(E entity, float actual, FloatRangeArgument expected, Text criterionName, Text entityName, boolean inverted) {
    final MutableText actualText = TextUtil.literal(actual).styled(TextUtil.STYLE_FOR_ACTUAL);
    final MutableText expectedText = Text.literal(StringUtil.wrapRange(expected)).styled(TextUtil.STYLE_FOR_EXPECTED);
    if (expected.isInRange(actual)) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.argument.entity_predicate.general.in_range", criterionName, entityName, actualText, expectedText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.argument.entity_predicate.general.out_of_range", criterionName, entityName, actualText, expectedText));
    }
  }

  static <E extends Entity> TestResult testDouble(E entity, double actual, NumberRange.FloatRange expected, Text criterionName, Text entityName, boolean inverted) {
    final MutableText actualText = TextUtil.literal(actual).styled(TextUtil.STYLE_FOR_ACTUAL);
    final MutableText expectedText = Text.literal(StringUtil.wrapRange(expected)).styled(TextUtil.STYLE_FOR_EXPECTED);
    if (expected.test(actual)) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.argument.entity_predicate.general.in_range", criterionName, entityName, actualText, expectedText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.argument.entity_predicate.general.out_of_range", criterionName, entityName, actualText, expectedText));
    }
  }
}
