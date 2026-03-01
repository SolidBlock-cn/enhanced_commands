package pers.solid.ecmd.mixins.general;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.config.DebugConfig;

@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
  /**
   * 如果启用了 {@link DebugConfig#noDarkSky}，则不会在低处将天空渲染为深色。
   *
   * @see DebugConfig#noDarkSky
   */
  //@Inject(method = "shouldRenderDarkDisc", at = @At("HEAD"), cancellable = true)
  private void neverRenderDarkDisc(float tickDelta, CallbackInfoReturnable<Boolean> cir) {
    if (DebugConfig.current.noDarkSky) {
      cir.cancel();
    }
  }
}
