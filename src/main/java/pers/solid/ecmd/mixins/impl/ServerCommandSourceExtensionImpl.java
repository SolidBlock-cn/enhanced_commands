package pers.solid.ecmd.mixins.impl;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pers.solid.ecmd.mixins.ext.ServerCommandSourceExtension;

import java.util.function.Supplier;

/**
 * 此 mixin 用于让 {@link ServerCommandSource} 实现 {@link ServerCommandSourceExtension}。
 */
@Mixin(ServerCommandSource.class)
public abstract class ServerCommandSourceExtensionImpl implements ServerCommandSourceExtension {
  @Shadow
  public abstract void sendFeedback(Supplier<Text> feedbackSupplier, boolean broadcastToOps);

  @Override
  public final void sendFeedback$ecBridge(Supplier<Text> feedbackSupplier, boolean broadcastToOps) {
    sendFeedback(feedbackSupplier, broadcastToOps);
  }
}
