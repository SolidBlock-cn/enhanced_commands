package pers.solid.ecmd.mixins.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.config.DebugConfig;

@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {

  /**
   * 如果 ignoreBorder 设置为 true，那么任何坐标都将被视为有效。
   */
  @Inject(method = "isWithinBounds(DDD)Z", at = @At("HEAD"), cancellable = true)
  private void forceContainPos(double x, double z, double margin, CallbackInfoReturnable<Boolean> cir) {
    if (DebugConfig.current.ignoreBorder) {
      cir.setReturnValue(true);
    }
  }

  /**
   * 如果 ignoreBorder 设置为 true，那么任何坐标都将被视为有效。
   */
  @Inject(method = "clampToBounds(DDD)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
  private void nullClamp(double x, double y, double z, CallbackInfoReturnable<BlockPos> cir) {
    if (DebugConfig.current.ignoreBorder) {
      cir.setReturnValue(BlockPos.containing(x, y, z));
    }
  }

  @Inject(method = "isInsideCloseToBorder", at = @At("HEAD"), cancellable = true)
  private void neverCollide(Entity entity, AABB box, CallbackInfoReturnable<Boolean> cir) {
    if (DebugConfig.current.ignoreBorder) {
      cir.setReturnValue(false);
    }
  }
}
