package pers.solid.ecmd.mixins.general;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pers.solid.ecmd.config.DebugConfig;
import pers.solid.ecmd.config.GameplayConfig;

@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
  @Shadow
  @Final
  private Minecraft minecraft;

  /**
   * 如果启用了 {@link DebugConfig#noDarkSky}，则不会在低处将天空渲染为深色。
   *
   * @see DebugConfig#noDarkSky
   */
  @Inject(method = "shouldRenderDarkDisc", at = @At("HEAD"), cancellable = true)
  private void neverRenderDarkDisc(float tickDelta, CallbackInfoReturnable<Boolean> cir) {
    if (DebugConfig.current.noDarkSky) {
      cir.cancel();
    }
  }

  @ModifyVariable(method = "setupRender", at = @At("HEAD"), index = 4, argsOnly = true)
  private boolean treatGhostPlayersAsSpectator(boolean isSpectator) {
    assert minecraft.player != null; // 原始代码中直接使用了 minecraft.player
    return DebugConfig.current.clearVisionInsideBlocks || (GameplayConfig.current.flyThroughBlocks && minecraft.player.getAbilities().flying) || DebugConfig.current.ghostPlayers || isSpectator;
  }
}
