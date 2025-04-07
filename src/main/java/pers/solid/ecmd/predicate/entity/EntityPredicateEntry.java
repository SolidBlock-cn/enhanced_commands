package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.BridgeDoubleRange;
import pers.solid.ecmd.util.bridge.BridgeFloatRange;
import pers.solid.ecmd.util.bridge.BridgeIntRange;

/**
 * 测试实体的某一特定属性的谓词信息，可在测试时提供详细的文本描述，以及对应的字符串内的表示形式。
 */
public interface EntityPredicateEntry extends EntityPredicate {
  /**
   * 将此谓词转换为实体选择器中的选项的形式，例如 {@code key=probability} 或 {@code key=!probability}。特定情况下可返回 {@code null}。
   */
  @Nullable String toOptionEntry();

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
  static <E extends Entity> TestResult testInt(E entity, int actual, BridgeIntRange expected, Text criterionName, Text entityName, boolean inverted) {
    final MutableText actualText = TextUtil.literal(actual).styled(Styles.ACTUAL);
    final MutableText expectedText = Text.literal(expected.asString()).styled(Styles.EXPECTED);
    if (expected.test(actual)) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.general.in_range", criterionName, entityName, actualText, expectedText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.general.out_of_range", criterionName, entityName, actualText, expectedText));
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
  static <E extends Entity> TestResult testFloat(E entity, float actual, BridgeFloatRange expected, Text criterionName, Text entityName, boolean inverted) {
    final MutableText actualText = TextUtil.literal(actual).styled(Styles.ACTUAL);
    final MutableText expectedText = Text.literal(expected.asString()).styled(Styles.EXPECTED);
    if (expected.test(actual)) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.general.in_range", criterionName, entityName, actualText, expectedText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.general.out_of_range", criterionName, entityName, actualText, expectedText));
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
  static <E extends Entity> TestResult testDouble(E entity, double actual, BridgeDoubleRange expected, Text criterionName, Text entityName, boolean inverted) {
    final MutableText actualText = TextUtil.literal(actual).styled(Styles.ACTUAL);
    final MutableText expectedText = Text.literal(expected.asString()).styled(Styles.EXPECTED);
    if (expected.test(actual)) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.general.in_range", criterionName, entityName, actualText, expectedText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.general.out_of_range", criterionName, entityName, actualText, expectedText));
    }
  }
}
