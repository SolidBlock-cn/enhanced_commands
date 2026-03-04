package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.api.EventBridges;
import pers.solid.ecmd.config.DebugConfig;

@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
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


  @SuppressWarnings({"UnresolvedMixinReference", "MixinAnnotationTarget"})
  @Inject(method = "lambda$addMainPass$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V", ordinal = 0), require = 0)
  private void injectedBeforeDebugRender(FogParameters fogParameters, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profilerFiller, Matrix4f matrix4f, Matrix4f matrix4f2, ResourceHandle<RenderTarget> resourceHandle, ResourceHandle<RenderTarget> resourceHandle2, ResourceHandle<RenderTarget> resourceHandle3, ResourceHandle<RenderTarget> resourceHandle4, Frustum frustum, boolean b, ResourceHandle<RenderTarget> resourceHandle5, CallbackInfo ci, @Local PoseStack poseStack, @Local(ordinal = 0) MultiBufferSource.BufferSource bufferSource) {
    // this mixin only applies to NeoForge
    EventBridges.INSTANCE.hookBeforeDebugRender((LevelRenderer) (Object) this, deltaTracker, camera, ci, poseStack, frustum, bufferSource, profilerFiller);
  }
}
