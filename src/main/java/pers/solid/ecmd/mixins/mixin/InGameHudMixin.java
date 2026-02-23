package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import pers.solid.ecmd.command.DebugIgnoreBoundaryCommand;

@Environment(EnvType.CLIENT)
@Mixin(Gui.class)
public abstract class InGameHudMixin {
  /**
   * 当 ignoreBorder 启用时，距离阈值设置为负数，从而始终不会渲染表示 warning 的红色晕影。
   *
   * @see DebugIgnoreBoundaryCommand#ignoreBorder
   */
  @ModifyExpressionValue(method = "renderVignette", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D"), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder;getWarningBlocks()I")))
  private double skipBorderWarning(double original) {
    if (DebugIgnoreBoundaryCommand.ignoreBorder) {
      return -999;
    }
    return original;
  }
}
