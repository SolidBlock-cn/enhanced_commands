package pers.solid.ecmd.command;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.EnhancedCommands;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public record TestResult(boolean successes, @Unmodifiable List<Text> descriptions, @Unmodifiable List<TestResult> attachments) {
  public TestResult(boolean booleanValue, Text descriptions) {
    this(booleanValue, List.of(descriptions), List.of());
  }

  public TestResult(boolean booleanValue, List<Text> descriptions) {
    this(booleanValue, descriptions, List.of());
  }

  public static TestResult success(BlockPos blockPos) {
    return new TestResult(true, Text.translatable("enhancedCommands.argument.blockPredicate.pass", EnhancedCommands.wrapBlockPos(blockPos)).formatted(Formatting.GREEN));
  }

  public static TestResult fail(BlockPos blockPos) {
    return new TestResult(false, Text.translatable("enhancedCommands.argument.blockPredicate.fail", EnhancedCommands.wrapBlockPos(blockPos)).formatted(Formatting.RED));
  }

  public static TestResult successOrFail(boolean successes, BlockPos blockPos) {
    return successes ? success(blockPos) : fail(blockPos);
  }

  public List<Text> descriptions() {
    return Collections.unmodifiableList(descriptions);
  }

  @Override
  public List<TestResult> attachments() {
    return Collections.unmodifiableList(attachments);
  }

  public void sendMessage(Consumer<Text> messageSender, int level) {
    for (Text text : descriptions) {
      if (level <= 0) {
        messageSender.accept(text);
      } else if (level <= 6) {
        messageSender.accept(Text.literal(StringUtils.repeat(' ', 2 * (level))).append(text));
      }
    }
    for (TestResult attachment : attachments) {
      attachment.sendMessage(messageSender, level + 1);
    }
  }

  public void sendMessage(ServerCommandSource serverCommandSource) {
    sendMessage(text -> serverCommandSource.sendFeedback(text, false), 0);
  }
}
