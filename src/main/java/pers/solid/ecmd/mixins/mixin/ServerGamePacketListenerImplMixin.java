package pers.solid.ecmd.mixins.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.command.DebugIgnoreBoundaryCommand;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
  /**
   * 当 ignoreBoundary 设置为 true 时，无视水平界限的限制。
   *
   * @see DebugIgnoreBoundaryCommand#ignoreBoundary
   */
  @Inject(method = "clampHorizontal", at = @At("HEAD"), cancellable = true)
  private static void noClampHorizontal(double d, CallbackInfoReturnable<Double> cir) {
    if (DebugIgnoreBoundaryCommand.ignoreBoundary) {
      cir.setReturnValue(d);
    }
  }

  /**
   * 当 ignoreBoundary 设置为 true 时，无视垂直界限的限制。
   *
   * @see DebugIgnoreBoundaryCommand#ignoreBoundary
   */
  @Inject(method = "clampVertical", at = @At("HEAD"), cancellable = true)
  private static void noClampVertical(double d, CallbackInfoReturnable<Double> cir) {
    if (DebugIgnoreBoundaryCommand.ignoreBoundary) {
      cir.setReturnValue(d);
    }
  }
}
