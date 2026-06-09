package pers.solid.ecmd.mixins.general;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.config.DebugConfig;
import pers.solid.ecmd.config.GameplayConfig;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
  @Shadow
  public ServerPlayer player;

  /**
   * 当 ignoreBoundary 设置为 true 时，无视水平界限的限制。
   *
   * @see DebugConfig#ignoreBoundary
   */
  @Inject(method = "clampHorizontal", at = @At("HEAD"), cancellable = true)
  private static void noClampHorizontal(double d, CallbackInfoReturnable<Double> cir) {
    if (DebugConfig.current.ignoreBoundary) {
      cir.setReturnValue(d);
    }
  }

  /**
   * 当 ignoreBoundary 设置为 true 时，无视垂直界限的限制。
   *
   * @see DebugConfig#ignoreBoundary
   */
  @Inject(method = "clampVertical", at = @At("HEAD"), cancellable = true)
  private static void noClampVertical(double d, CallbackInfoReturnable<Double> cir) {
    if (DebugConfig.current.ignoreBoundary) {
      cir.setReturnValue(d);
    }
  }

  @Inject(method = "isPlayerCollidingWithAnythingNew", at = @At("HEAD"), cancellable = true)
  private void noCollision(LevelReader level, AABB box, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
    if (DebugConfig.current.playersNoCollision || DebugConfig.current.entitiesNoCollision) {
      cir.setReturnValue(false);
    }
    if (GameplayConfig.current.flyThroughBlocks && player.getAbilities().flying) {
      cir.setReturnValue(false);
    }
  }
}
