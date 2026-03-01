package pers.solid.ecmd.api;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MethodsReturnNonnullByDefault
public interface EventBridges {
  EventBridges INSTANCE = getInstance();

  @ExpectPlatform
  static EventBridges getInstance() {
    throw new AssertionError();
  }

  EventBridge<UseBlockCallbackBridge> useBlockEvent();

  EventBridge<AttackBlockCallbackBridge> attackBlockEvent();

  default void hookBeforeDebugRender(LevelRenderer levelRenderer, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci, PoseStack poseStack, Frustum frustum, MultiBufferSource.BufferSource multiBufferSource, ProfilerFiller profilerFiller, ClientLevel level) {
    // 在 Fabric 中，不需要做任何事，因为有 Fabric API 提供的事件。
    // 在 NeoForge 中，需要单独处理。
  }

  interface UseBlockCallbackBridge {
    InteractionResult interact(Player player, Level world, InteractionHand hand, BlockHitResult hitResult);
  }

  interface AttackBlockCallbackBridge {
    InteractionResult interact(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction);
  }
}
