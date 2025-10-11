package pers.solid.ecmd.mixins.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.command.DebugIgnoreBoundaryCommand;

@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {

  /**
   * 如果 ignoreBorder 设置为 true，那么任何坐标都将被视为有效。
   */
  @Inject(method = "contains(DDD)Z", at = @At("HEAD"), cancellable = true)
  private void forceContainPos(double x, double z, double margin, CallbackInfoReturnable<Boolean> cir) {
    if (DebugIgnoreBoundaryCommand.ignoreBorder) {
      cir.setReturnValue(true);
    }
  }

  /**
   * 如果 ignoreBorder 设置为 true，那么任何坐标都将被视为有效。
   */
  @Inject(method = "clamp(DDD)Lnet/minecraft/util/math/BlockPos;", at = @At("HEAD"), cancellable = true)
  private void nullClamp(double x, double y, double z, CallbackInfoReturnable<BlockPos> cir) {
    if (DebugIgnoreBoundaryCommand.ignoreBorder) {
      cir.setReturnValue(BlockPos.ofFloored(x, y, z));
    }
  }

  @Inject(method = "canCollide", at = @At("HEAD"), cancellable = true)
  private void neverCollide(Entity entity, Box box, CallbackInfoReturnable<Boolean> cir) {
    if (DebugIgnoreBoundaryCommand.ignoreBorder) {
      cir.setReturnValue(false);
    }
  }
}
