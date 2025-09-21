package pers.solid.ecmd.mixins.ext;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.PositionProvider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 此接口将注入至 {@link ServerCommandSource}。
 *
 * @see pers.solid.ecmd.mixins.impl.ServerCommandSourceExtensionImpl
 */
public interface ServerCommandSourceExtension extends PositionProvider {
  /**
   * 在 1.20 之前，第一个参数是 {@code Text}，而自 1.20 之后，第一个参数调整为 {@code Supplier<Text>}，为减少在不同版本之间转换的成本，在这里做个桥梁方法。请优先使用此方法。
   */
  default void sendFeedback$ecBridge(Supplier<Text> feedbackSupplier, boolean broadcastToOps) {
    ((ServerCommandSource) this).sendFeedback(feedbackSupplier, broadcastToOps);
  }

  @NotNull
  Map<String, Object> getExtraArguments$ec();

  default void addExtraArgument$ec(String name, Object argument) {
    getExtraArguments$ec().put(name, argument);
  }

  default <T> T getExtraArgument$ec(String name, Class<T> type) {
    final Object o = getExtraArguments$ec().get(name);
    try {
      return type.cast(o);
    } catch (ClassCastException c) {
      EnhancedCommands.LOGGER.error("Argument '{}' is not a correct type!", name, c);
      return null;
    }
  }
}
