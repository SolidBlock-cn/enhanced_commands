package pers.solid.ecmd.mixins.neoforge;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.api.neoforge.BeforeDebugRenderEvent;

@OnlyIn(Dist.CLIENT)
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
  @Inject(method = "lambda$addMainPass$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V", ordinal = 0), require = 0)
  private void injectedBeforeDebugRender(FogParameters fogParameters, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profilerFiller, Matrix4f matrix4f, Matrix4f matrix4f2, ResourceHandle<RenderTarget> resourceHandle, ResourceHandle<RenderTarget> resourceHandle2, ResourceHandle<RenderTarget> resourceHandle3, ResourceHandle<RenderTarget> resourceHandle4, Frustum frustum, boolean b, ResourceHandle<RenderTarget> resourceHandle5, CallbackInfo ci, @Local(name = "posestack") PoseStack poseStack, @Local(name = "multibuffersource$buffersource") MultiBufferSource.BufferSource bufferSource) {
    final BeforeDebugRenderEvent event = new BeforeDebugRenderEvent((LevelRenderer) (Object) this, deltaTracker, poseStack, camera, frustum, bufferSource, profilerFiller);
    NeoForge.EVENT_BUS.post(event);
  }
}
