package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.FogRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import pers.solid.ecmd.config.DebugConfig;

@Environment(EnvType.CLIENT)
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
  /**
   * 在启用了 {@link DebugConfig#noDarkSky} 的情况下，不将雾的颜色加深。
   */
  @ModifyExpressionValue(method = "setupColor", at = @At(value = "FIELD", target = "Lnet/minecraft/world/phys/Vec3;y:D", opcode = Opcodes.GETFIELD), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getThunderLevel(F)F"), to = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getMinBuildHeight()I")))
  private static double noDarkFogColor(double original) {
    if (DebugConfig.current.noDarkSky) {
      return Math.max(128, original);
    }
    return original;
  }
}
