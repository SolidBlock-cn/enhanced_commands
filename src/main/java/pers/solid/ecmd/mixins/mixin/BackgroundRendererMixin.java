package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BackgroundRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.config.DebugConfig;

@Environment(EnvType.CLIENT)
@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
  /**
   * 在启用了 {@link DebugConfig#noDarkSky} 的情况下，不将雾的颜色加深。
   */
  @ModifyExpressionValue(method = "getFogColor", at = @At(value = "FIELD", target = "Lnet/minecraft/util/math/Vec3d;y:D"))
  private static double noDarkFogColor(double original) {
    if (DebugConfig.current.noDarkSky) {
      return Math.max(128, original);
    }
    return original;
  }
}
