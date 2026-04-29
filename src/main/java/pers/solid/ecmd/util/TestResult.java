package pers.solid.ecmd.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.block.predicate.AllBlockPredicate;
import pers.solid.ecmd.block.predicate.BlockPredicate;

import java.util.ArrayList;
import java.util.List;

/**
 * 包含测试结果、说明文字以及附加的说明文字的记录。一些测试是通过多个测试组合起来的，例如 {@link AllBlockPredicate} 就涉及多个 {@link BlockPredicate} 的结果的组合，因此会使用 {@code attachments}。
 *
 * @param successes    测试的结果，描述此次测试是否成功。
 * @param descriptions 对本次测试结果的直接描述。
 * @param attachments  本次测试结果如果含有其他的一些测试，那么此类将包含相应的其他测试的结果。
 */
public record TestResult(boolean successes, @Unmodifiable List<Component> descriptions, @Unmodifiable List<TestResult> attachments) {
  private static final DynamicCommandExceptionType EXCEPTION_TYPE = new DynamicCommandExceptionType(testResult -> ((TestResult) testResult).createText());

  public TestResult(boolean booleanValue, Component description) {
    this(booleanValue, List.of(description), List.of());
  }

  public TestResult(boolean booleanValue, List<Component> descriptions) {
    this(booleanValue, descriptions, List.of());
  }

  /**
   * 创建 {@link TestResult} 对象，同时根据其 {@code successes} 的值给文本添加绿色或红色。注意会直接对此参数进行修改。
   */
  public static TestResult of(boolean successes, MutableComponent description) {
    return new TestResult(successes, description.withStyle(Styles.trueOrFalse(successes)));
  }

  /**
   * 创建 {@link TestResult} 对象，同时根据其 {@code successes} 的值给文本添加绿色或红色。注意会直接对此参数进行修改。
   */
  public static TestResult of(boolean successes, MutableComponent description, @Unmodifiable List<TestResult> attachments) {
    return new TestResult(successes, List.of(description.withStyle(Styles.trueOrFalse(successes))), attachments);
  }

  @Contract(mutates = "param1")
  public void appendTexts(List<Component> lines, int level) {
    for (Component text : descriptions) {
      if (level <= 0) {
        lines.add(text);
      } else if (level <= 6) {
        lines.add(Component.literal(StringUtils.repeat(' ', 2 * (level))).append(text));
      }
    }
    for (TestResult attachment : attachments) {
      attachment.appendTexts(lines, level + 1);
    }
  }

  /**
   * 将结果转化为文本组件。
   */
  public Component createText() {
    final List<Component> lines = new ArrayList<>();
    appendTexts(lines, 0);
    return CommonComponents.joinLines(lines);
  }

  public void makeFeedback(CommandSourceStack serverCommandSource) throws CommandSyntaxException {
    if (successes()) {
      serverCommandSource.sendFeedback$ecBridge(this::createText, false);
    } else {
      throw EXCEPTION_TYPE.create(this);
    }
  }
}
