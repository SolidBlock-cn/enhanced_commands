package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.config.DebugConfig;

@Environment(EnvType.CLIENT)
@Mixin(FogRenderer.class)
public abstract class BackgroundRendererMixin {
  /**
   * 在启用了 {@link DebugConfig#noDarkSky} 的情况下，不将雾的颜色加深。
   */
  @ModifyExpressionValue(method = "computeFogColor", at = @At(value = "FIELD", target = "Lnet/minecraft/world/phys/Vec3;y:D"))
  private static double noDarkFogColor(double original) {
    if (DebugConfig.current.noDarkSky) {
      return Math.max(128, original);
    }
    return original;
  }
}
