package pers.solid.ecmd.mixins.mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.command.DebugIgnoreBoundaryCommand;

@Mixin(World.class)
public abstract class WorldMixin {
  /**
   * 当 ignore boundary 设置为 true 时，方块坐标在水平方向上始终是有效的。
   *
   * @see DebugIgnoreBoundaryCommand#ignoreBoundary
   */
  @Inject(method = "isValidHorizontally", at = @At("HEAD"), cancellable = true)
  private static void forceValidHorizontally(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
    if (DebugIgnoreBoundaryCommand.ignoreBoundary) {
      cir.setReturnValue(true);
    }
  }

  /**
   * 当 ignore boundary 设置为 true 时，方块坐标在垂直方向上始终是有效的。
   *
   * @see DebugIgnoreBoundaryCommand#ignoreBoundary
   */
  @Inject(method = "isInvalidVertically", at = @At("HEAD"), cancellable = true)
  private static void forceValidVertically(int y, CallbackInfoReturnable<Boolean> cir) {
    if (DebugIgnoreBoundaryCommand.ignoreBoundary) {
      cir.setReturnValue(false);
    }
  }
}
