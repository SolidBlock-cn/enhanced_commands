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

/**
 * 测试实体的某一特定属性的谓词信息，可在测试时提供详细的文本描述，以及对应的字符串内的表示形式。
 */
public interface EntityPredicateEntry extends EntityPredicate {
  @Override
  default boolean test(Entity entity) {
    return false;
  }

  /**
   * 测试实体并返回描述信息。调用时请使用此类，但覆盖时请覆盖 {@link #testAndDescribe(Entity, Text)}。
   */
  @ApiStatus.NonExtendable
  default TestResult testAndDescribe(Entity entity) throws CommandSyntaxException {
    return testAndDescribe(entity, TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET));
  }

  /**
   * 测试实体并返回描述信息，实现接口应覆盖此方法，但通常不要直接调用此方法，但是如果需要对同一个实体多次调用此方法，则可以使用此方法并共用 {@code displayName} 参数。使用 {@code displayName} 是考虑到其会被多次用到，为了避免多次创建其对象而直接使用共用的此对象。
   *
   * @param entity      被测试的实体。
   * @param displayName 被测试的实体的显示名称。
   */
  TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException;

  /**
   * 将此谓词转换为实体选择器中的选项的形式，例如 {@code key=value} 或 {@code key=!value}.
   */
  String toOptionEntry();

  /**
   * 测试实体的某个整数属性，并返回 {@link TestResult} 以描述其值是否在指定的范围内。
   *
   * @param entity        被测试的实体。
   * @param actual        被测试的实体的某个属性的实际值。
   * @param expected      预期值的整数范围。
   * @param criterionName 需要测试的属性的名称，会显示在测试结果中。
   * @param entityName    实体的显示名称，会显示在测试结果中。
   * @param inverted      测试是否为反向的，不影响结果的文本内容，但是会影响结果的真假判断。
   */
  static <E extends Entity> TestResult testInt(E entity, int actual, NumberRange.IntRange expected, Text criterionName, Text entityName, boolean inverted) {
    final MutableText actualText = TextUtil.literal(actual).styled(TextUtil.STYLE_FOR_ACTUAL);
    final MutableText expectedText = Text.literal(StringUtil.wrapRange(expected)).styled(TextUtil.STYLE_FOR_EXPECTED);
    if (expected.test(actual)) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.argument.entity_predicate.general.in_range", criterionName, entityName, actualText, expectedText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.argument.entity_predicate.general.out_of_range", criterionName, entityName, actualText, expectedText));
    }
  }

  /**
   * 测试实体的某个浮点数属性，并返回 {@link TestResult} 以描述其值是否在指定的范围内。
   *
   * @param entity        被测试的实体。
   * @param actual        被测试的实体的某个属性的实际值。
   * @param expected      预期值的整数范围。
   * @param criterionName 需要测试的属性的名称，会显示在测试结果中。
   * @param entityName    实体的显示名称，会显示在测试结果中。
   * @param inverted      测试是否为反向的，不影响结果的文本内容，但是会影响结果的真假判断。
   */
  static <E extends Entity> TestResult testFloat(E entity, float actual, FloatRangeArgument expected, Text criterionName, Text entityName, boolean inverted) {
    final MutableText actualText = TextUtil.literal(actual).styled(TextUtil.STYLE_FOR_ACTUAL);
    final MutableText expectedText = Text.literal(StringUtil.wrapRange(expected)).styled(TextUtil.STYLE_FOR_EXPECTED);
    if (expected.isInRange(actual)) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.argument.entity_predicate.general.in_range", criterionName, entityName, actualText, expectedText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.argument.entity_predicate.general.out_of_range", criterionName, entityName, actualText, expectedText));
    }
  }

  /**
   * 测试实体的某个双精度浮点数属性，并返回 {@link TestResult} 以描述其值是否在指定的范围内。
   *
   * @param entity        被测试的实体。
   * @param actual        被测试的实体的某个属性的实际值。
   * @param expected      预期值的整数范围。
   * @param criterionName 需要测试的属性的名称，会显示在测试结果中。
   * @param entityName    实体的显示名称，会显示在测试结果中。
   * @param inverted      测试是否为反向的，不影响结果的文本内容，但是会影响结果的真假判断。
   */
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
