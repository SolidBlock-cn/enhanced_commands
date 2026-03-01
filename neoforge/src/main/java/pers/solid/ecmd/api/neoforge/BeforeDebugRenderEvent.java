package pers.solid.ecmd.api.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.Event;
import org.joml.Matrix4f;

public class BeforeDebugRenderEvent extends Event {
  public final LevelRenderer levelRenderer;
  public final DeltaTracker deltaTracker;
  public final PoseStack poseStack;
  public final Camera camera;
  public final Frustum frustum;
  public final GameRenderer gameRenderer;
  public final LightTexture lightTexture;
  public final Matrix4f projectionMatrix;
  public final Matrix4f positionMatrix;
  public final MultiBufferSource bufferSource;
  public final ProfilerFiller profiler;
  public final ClientLevel level;

  public BeforeDebugRenderEvent(LevelRenderer levelRenderer, DeltaTracker deltaTracker, PoseStack poseStack, Camera camera, Frustum frustum, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, Matrix4f positionMatrix, MultiBufferSource bufferSource, ProfilerFiller profiler, ClientLevel level) {
    this.levelRenderer = levelRenderer;
    this.deltaTracker = deltaTracker;
    this.poseStack = poseStack;
    this.camera = camera;
    this.frustum = frustum;
    this.gameRenderer = gameRenderer;
    this.lightTexture = lightTexture;
    this.projectionMatrix = projectionMatrix;
    this.positionMatrix = positionMatrix;
    this.bufferSource = bufferSource;
    this.profiler = profiler;
    this.level = level;
  }
}
