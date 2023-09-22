package pers.solid.ecmd.util.bridge;

import com.google.common.base.Suppliers;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
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
    source.sendFeedback(new LazyText(text), broadcastToOps);
  }

  /**
   * @see Suppliers#memoize(com.google.common.base.Supplier)
   */
  private static class LazyText implements Text {
    private Supplier<@NotNull Text> supplier;
    private Text value;

    private LazyText(@NotNull Supplier<@NotNull Text> supplier) {
      this.supplier = supplier;
    }

    private @NotNull Text get() {
      if (value == null) {
        synchronized (this) {
          value = Objects.requireNonNull(supplier.get(), "The Supplier<Text> must not return a null value");
          supplier = null;
        }
      }
      return value;
    }

    @Override
    public Style getStyle() {
      return get().getStyle();
    }

    @Override
    public TextContent getContent() {
      return get().getContent();
    }

    @Override
    public List<Text> getSiblings() {
      return get().getSiblings();
    }

    @Override
    public OrderedText asOrderedText() {
      return get().asOrderedText();
    }
  }
}
