package pers.solid.ecmd.mixins.general;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import pers.solid.ecmd.config.DebugConfig;

@Environment(EnvType.CLIENT)
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
  /**
   * 在启用了 {@link DebugConfig#noDarkSky} 的情况下，不将雾的颜色加深。
   */
  //@ModifyExpressionValue(method = "computeFogColor", at = @At(value = "FIELD", target = "Lnet/minecraft/world/phys/Vec3;y:D", opcode = Opcodes.GETFIELD))
  private static double noDarkFogColor(double original) {
    if (DebugConfig.current.noDarkSky) {
      return Math.max(128, original);
    }
    return original;
  }
}
