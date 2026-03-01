package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.api.EventBridges;
import pers.solid.ecmd.config.DebugConfig;

@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
  @Shadow
  @Nullable
  private ClientLevel level;

  /**
   * 如果启用了 {@link DebugConfig#noDarkSky}，则不会在低处将天空渲染为深色。
   *
   * @return
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

  @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V"))
  private void injectedBeforeDebugRender(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci, @Local PoseStack poseStack, @Local Frustum frustum, @Local MultiBufferSource.BufferSource bufferSource, @Local ProfilerFiller profilerFiller) {
    EventBridges.INSTANCE.hookBeforeDebugRender((LevelRenderer) (Object) this, deltaTracker, renderBlockOutline, camera, gameRenderer, lightTexture, frustumMatrix, projectionMatrix, ci, poseStack, frustum, bufferSource, profilerFiller, level);
  }
}
