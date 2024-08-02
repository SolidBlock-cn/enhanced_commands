package pers.solid.ecmd.mixins.impl;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.mixins.ext.ServerCommandSourceExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 此 mixin 用于让 {@link ServerCommandSource} 实现 {@link ServerCommandSourceExtension}。
 */
@Mixin(ServerCommandSource.class)
public abstract class ServerCommandSourceExtensionImpl implements ServerCommandSourceExtension {
  @Unique
  private Map<String, Object> extraArguments = null;

  @Shadow
  public abstract void sendFeedback(Supplier<Text> feedbackSupplier, boolean broadcastToOps);

  @Override
  public final void sendFeedback$ecBridge(Supplier<Text> feedbackSupplier, boolean broadcastToOps) {
    sendFeedback(feedbackSupplier, broadcastToOps);
  }

  @Override
  public @NotNull Map<String, Object> getExtraArguments$ec() {
    return Objects.requireNonNullElseGet(extraArguments, Map::of);
  }

  @Override
  public void addExtraArgument$ec(String name, Object argument) {
    if (extraArguments == null) {
      extraArguments = new HashMap<>();
    }
    extraArguments.put(name, argument);
  }
}
