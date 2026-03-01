package pers.solid.ecmd.mixins.general;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.config.DebugConfig;

@Mixin(Level.class)
public abstract class LevelMixin {
  /**
   * 当 ignore boundary 设置为 true 时，方块坐标在水平方向上始终是有效的。
   *
   * @see DebugConfig#ignoreBoundary
   */
  @Inject(method = "isInWorldBoundsHorizontal", at = @At("HEAD"), cancellable = true)
  private static void forceValidHorizontally(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
    if (DebugConfig.current.ignoreBoundary) {
      cir.setReturnValue(true);
    }
  }

  /**
   * 当 ignore boundary 设置为 true 时，方块坐标在垂直方向上始终是有效的。
   *
   * @see DebugConfig#ignoreBoundary
   */
  @Inject(method = "isOutsideSpawnableHeight", at = @At("HEAD"), cancellable = true)
  private static void forceValidVertically(int y, CallbackInfoReturnable<Boolean> cir) {
    if (DebugConfig.current.ignoreBoundary) {
      cir.setReturnValue(false);
    }
  }
}
