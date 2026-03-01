package pers.solid.ecmd.api.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.api.EventBridge;
import pers.solid.ecmd.api.EventBridges;

@MethodsReturnNonnullByDefault
public enum EventBridgesImpl implements EventBridges {
  INSTANCE;
  public static final EventBridgeImpl.FromEventBus<UseBlockCallbackBridge, PlayerInteractEvent.RightClickBlock> USE_BLOCK = new EventBridgeImpl.FromEventBus<>(PlayerInteractEvent.RightClickBlock.class, useBlockCallbackBridge -> rightClickBlock -> {
    final InteractionResult interact = useBlockCallbackBridge.interact(rightClickBlock.getEntity(), rightClickBlock.getLevel(), rightClickBlock.getHand(), rightClickBlock.getHitVec());
    rightClickBlock.setCancellationResult(interact);
  }, rightClickBlockConsumer -> (player, world, hand, hitResult) -> {
    final PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player, hand, hitResult.getBlockPos(), hitResult);
    rightClickBlockConsumer.accept(event);
    return event.getCancellationResult();
  });
  public static final EventBridgeImpl.FromEventBus<AttackBlockCallbackBridge, PlayerInteractEvent.LeftClickBlock> ATTACK_BLOCK = new EventBridgeImpl.FromEventBus<>(PlayerInteractEvent.LeftClickBlock.class, attackBlockCallbackBridge -> leftClickBlock -> {
    final InteractionResult interact = attackBlockCallbackBridge.interact(leftClickBlock.getEntity(), leftClickBlock.getLevel(), leftClickBlock.getHand(), leftClickBlock.getPos(), leftClickBlock.getFace());
    leftClickBlock.setCanceled(interact.consumesAction());
  }, leftClickBlockConsumer -> (player, world, hand, pos, direction) -> {
    final PlayerInteractEvent.LeftClickBlock event = new PlayerInteractEvent.LeftClickBlock(player, pos, direction, PlayerInteractEvent.LeftClickBlock.Action.START);
    leftClickBlockConsumer.accept(event);
    return event.isCanceled() ? InteractionResult.FAIL : InteractionResult.SUCCESS;
  });

  public static EventBridges getInstance() {
    return INSTANCE;
  }

  @Override
  public EventBridge<UseBlockCallbackBridge> useBlockEvent() {
    return USE_BLOCK;
  }

  @Override
  public EventBridge<AttackBlockCallbackBridge> attackBlockEvent() {
    return ATTACK_BLOCK;
  }

  @Override
  public void hookBeforeDebugRender(LevelRenderer levelRenderer, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci, PoseStack poseStack, Frustum frustum, MultiBufferSource.BufferSource multiBufferSource, ProfilerFiller profilerFiller, ClientLevel level) {
    final BeforeDebugRenderEvent event = new BeforeDebugRenderEvent(levelRenderer, deltaTracker, poseStack, camera, frustum, gameRenderer, lightTexture, projectionMatrix, frustumMatrix, multiBufferSource, profilerFiller, level);
    NeoForge.EVENT_BUS.post(event);
  }
}
