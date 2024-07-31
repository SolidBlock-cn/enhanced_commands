package pers.solid.ecmd.util.bridge;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * <p>用于连接不同版本中与命令有关的方法。
 * <p>从 1.20 开始，<code>source.sendFeedback</code> 的第二个参数由 {@code Text} 改成了 {@code Supplier<Text>}。
 */
public final class CommandBridge {
  private CommandBridge() {}

  /**
   * @see #sendFeedback(ServerCommandSource, Supplier, boolean)
   * @deprecated Please do not create {@link Text} objects when no outputs are needed.
   */
  @Deprecated
  public static void sendFeedback(@NotNull ServerCommandSource source, Text text, boolean broadcastToOps) {
    source.sendFeedback(() -> text, broadcastToOps);
  }

  public static void sendFeedback(@NotNull CommandContext<ServerCommandSource> context, Supplier<@NotNull Text> text, boolean broadcastToOps) {
    sendFeedback(context.getSource(), text, broadcastToOps);
  }

  public static void sendFeedback(@NotNull ServerCommandSource source, Supplier<@NotNull Text> text, boolean broadcastToOps) {
    source.sendFeedback(text, broadcastToOps);
  }
}
