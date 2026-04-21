package pers.solid.ecmd.entity.predicate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
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
   * 测试实体的某个整数属性，并返回 {@link TestResult} 以描述其值是否在指定的范围内。
   *
   * @param entity        被测试的实体。
   * @param actual        被测试的实体的某个属性的实际值。
   * @param expected      预期值的整数范围。
   * @param criterionName 需要测试的属性的名称，会显示在测试结果中。
   * @param entityName    实体的显示名称，会显示在测试结果中。
   * @param inverted      测试是否为反向的，不影响结果的文本内容，但是会影响结果的真假判断。
   */
  static <E extends Entity> TestResult testInt(E entity, int actual, BridgeIntRange expected, Component criterionName, Component entityName, boolean inverted) {
    final MutableComponent actualText = TextUtil.literal(actual).withStyle(Styles.ACTUAL);
    final MutableComponent expectedText = Component.literal(expected.expressAsString()).withStyle(Styles.EXPECTED);
    if (expected.test(actual)) {
      return TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.general.in_range", criterionName, entityName, actualText, expectedText));
    } else {
      return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.general.out_of_range", criterionName, entityName, actualText, expectedText));
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
  static <E extends Entity> TestResult testFloat(E entity, float actual, BridgeFloatRange expected, Component criterionName, Component entityName, boolean inverted) {
    final MutableComponent actualText = TextUtil.literal(actual).withStyle(Styles.ACTUAL);
    final MutableComponent expectedText = Component.literal(expected.expressAsString()).withStyle(Styles.EXPECTED);
    if (expected.test(actual)) {
      return TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.general.in_range", criterionName, entityName, actualText, expectedText));
    } else {
      return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.general.out_of_range", criterionName, entityName, actualText, expectedText));
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
  static <E extends Entity> TestResult testDouble(E entity, double actual, BridgeDoubleRange expected, Component criterionName, Component entityName, boolean inverted) {
    final MutableComponent actualText = TextUtil.literal(actual).withStyle(Styles.ACTUAL);
    final MutableComponent expectedText = Component.literal(expected.expressAsString()).withStyle(Styles.EXPECTED);
    if (expected.test(actual)) {
      return TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.general.in_range", criterionName, entityName, actualText, expectedText));
    } else {
      return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.general.out_of_range", criterionName, entityName, actualText, expectedText));
    }
  }

  /**
   * 将此谓词转换为实体选择器中的选项的形式，例如 {@code key=probability} 或 {@code key=!probability}。特定情况下可返回 {@code null}。
   */
  @Nullable String toOptionEntry();

  @Override
  default String expressAsString() {
    return "[" + toOptionEntry() + "]";
  }
}
