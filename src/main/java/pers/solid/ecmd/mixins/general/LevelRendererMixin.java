package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
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
  @Definition(id = "d", local = @Local(type = double.class))
  @Expression("d < 0.0")
  @ModifyExpressionValue(method = "renderSky", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
  private boolean neverRenderDarkDisc(boolean original) {
    // 此处对应新版本中的 shouldRenderDarkDisc 方法。
    // 在当前版本中，shouldRenderDarkDisc 相当于是被内联的。
    if (DebugConfig.current.noDarkSky) {
      return false;
    }
    return original;
  }

  @ModifyVariable(method = "setupRender", at = @At("HEAD"), index = 4, argsOnly = true)
  private boolean treatGhostPlayersAsSpectator(boolean isSpectator) {
    assert minecraft.player != null; // 原始代码中直接使用了 minecraft.player
    return DebugConfig.current.clearVisionInsideBlocks || (GameplayConfig.current.flyThroughBlocks && minecraft.player.getAbilities().flying) || DebugConfig.current.ghostPlayers || isSpectator;
  }
}
