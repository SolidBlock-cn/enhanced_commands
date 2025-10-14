package pers.solid.ecmd.mixins.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.config.DebugConfig;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
  /**
   * 如果启用了 {@link DebugConfig#noDarkSky}，则不会在低处将天空渲染为深色。
   *
   * @see DebugConfig#noDarkSky
   */
  @Inject(method = "isSkyDark", at = @At("HEAD"), cancellable = true)
  private void neverSkyDark(float tickDelta, CallbackInfoReturnable<Boolean> cir) {
    if (DebugConfig.current.noDarkSky) {
      cir.cancel();
    }
  }
}
