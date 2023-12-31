package pers.solid.ecmd.command;

import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.bridge.CommandBridge;

import java.util.ArrayList;
import java.util.List;

public record TestResult(boolean successes, @Unmodifiable List<Text> descriptions, @Unmodifiable List<TestResult> attachments) {
  public TestResult(boolean booleanValue, Text description) {
    this(booleanValue, List.of(description), List.of());
  }

  public TestResult(boolean booleanValue, List<Text> descriptions) {
    this(booleanValue, descriptions, List.of());
  }

  /**
   * 创建 {@link TestResult} 对象，同时根据其 {@code successes} 的值给文本添加绿色或红色。注意会直接对此参数进行修改。
   */
  public static TestResult of(boolean successes, MutableText description) {
    return new TestResult(successes, description.styled(Styles.trueOrFalse(successes)));
  }

  /**
   * 创建 {@link TestResult} 对象，同时根据其 {@code successes} 的值给文本添加绿色或红色。注意会直接对此参数进行修改。
   */
  public static TestResult of(boolean successes, MutableText description, @Unmodifiable List<TestResult> attachments) {
    return new TestResult(successes, List.of(description.styled(Styles.trueOrFalse(successes))), attachments);
  }

  @Contract(mutates = "param1")
  public void appendTexts(List<Text> lines, int level) {
    for (Text text : descriptions) {
      if (level <= 0) {
        lines.add(text);
      } else if (level <= 6) {
        lines.add(Text.literal(StringUtils.repeat(' ', 2 * (level))).append(text));
      }
    }
    for (TestResult attachment : attachments) {
      attachment.appendTexts(lines, level + 1);
    }
  }

  public void sendMessage(ServerCommandSource serverCommandSource) {
    CommandBridge.sendFeedback(serverCommandSource, () -> {
      final List<Text> lines = new ArrayList<>();
      appendTexts(lines, 0);
      return ScreenTexts.joinLines(lines);
    }, false);
  }
}
