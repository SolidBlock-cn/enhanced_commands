package pers.solid.ecmd.api.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.Event;

@OnlyIn(Dist.CLIENT)
public class BeforeDebugRenderEvent extends Event {
  public final LevelRenderer levelRenderer;
  public final DeltaTracker deltaTracker;
  public final PoseStack poseStack;
  public final Camera camera;
  public final Frustum frustum;
  public final MultiBufferSource bufferSource;
  public final ProfilerFiller profiler;

  public BeforeDebugRenderEvent(LevelRenderer levelRenderer, DeltaTracker deltaTracker, PoseStack poseStack, Camera camera, Frustum frustum, MultiBufferSource bufferSource, ProfilerFiller profiler) {
    this.levelRenderer = levelRenderer;
    this.deltaTracker = deltaTracker;
    this.poseStack = poseStack;
    this.camera = camera;
    this.frustum = frustum;
    this.bufferSource = bufferSource;
    this.profiler = profiler;
  }
}
