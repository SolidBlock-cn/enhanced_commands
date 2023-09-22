package pers.solid.ecmd.util.bridge;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.mixin.ServerCommandSourceAccessor;

import java.util.function.Supplier;

public final class CommandBridge {
  private CommandBridge() {}

  /**
   * @see #sendFeedback(ServerCommandSource, Supplier, boolean)
   * @deprecated Please do not create {@link Text} objects when no outputs are needed.
   */
  @Deprecated
  public static void sendFeedback(@NotNull ServerCommandSource source, Text text, boolean broadcastToOps) {
    source.sendFeedback(text, broadcastToOps);
  }

  public static void sendFeedback(@NotNull CommandContext<ServerCommandSource> context, Supplier<@NotNull Text> text, boolean broadcastToOps) {
    sendFeedback(context.getSource(), text, broadcastToOps);
  }

  public static void sendFeedback(@NotNull ServerCommandSource source, Supplier<@NotNull Text> text, boolean broadcastToOps) {
    if (((ServerCommandSourceAccessor) source).isSilent()) return;
    final CommandOutput output = ((ServerCommandSourceAccessor) source).getOutput();
    if (output.shouldReceiveFeedback() || (broadcastToOps && output.shouldBroadcastConsoleToOps())) {
      source.sendFeedback(text.get(), broadcastToOps);
    }
  }
}
