package pers.solid.ecmd.mixins.ext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.mixins.impl.CommandSourceStackStackExtensionImpl;
import pers.solid.ecmd.util.PositionProvider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 此接口将注入至 {@link CommandSourceStack}。
 *
 * @see CommandSourceStackStackExtensionImpl
 */
public interface CommandSourceStackExtension extends PositionProvider {
  /**
   * 在 1.20 之前，第一个参数是 {@code Text}，而自 1.20 之后，第一个参数调整为 {@code Supplier<Text>}，为减少在不同版本之间转换的成本，在这里做个桥梁方法。请优先使用此方法。
   */
  default void sendFeedback$ecBridge(Supplier<Component> feedbackSupplier, boolean broadcastToOps) {
    ((CommandSourceStack) this).sendSuccess(feedbackSupplier, broadcastToOps);
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
