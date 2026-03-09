package pers.solid.ecmd.api.neoforge;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import pers.solid.ecmd.api.EventBridge;
import pers.solid.ecmd.api.EventBridges;
import pers.solid.ecmd.api.FlipStateCallback;

/**
 * {@link EventBridges} 在 NeoForge 中的实现。
 */
@MethodsReturnNonnullByDefault
public enum EventBridgesImpl implements EventBridges {
  INSTANCE;

  public static final EventBridgeImpl.FromEventBus<UseBlockCallbackBridge, PlayerInteractEvent.RightClickBlock> USE_BLOCK = new EventBridgeImpl.FromEventBus<>(PlayerInteractEvent.RightClickBlock.class, useBlockCallbackBridge -> rightClickBlock -> {
    final InteractionResult interact = useBlockCallbackBridge.interact(rightClickBlock.getEntity(), rightClickBlock.getLevel(), rightClickBlock.getHand(), rightClickBlock.getHitVec());
    if (interact.consumesAction()) {
      rightClickBlock.setUseBlock(TriState.FALSE);
    }
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

  public static final EventBridgeImpl.FromEventBus<FlipStateCallback, FlipStateEvent> FLIP_STATE = new EventBridgeImpl.FromEventBus<>(FlipStateEvent.class, flipStateCallback -> flipStateEvent -> {
    final BlockState flippedState = flipStateCallback.getFlippedState(flipStateEvent.getIntermediateState(), flipStateEvent.getOriginalState());
    flipStateEvent.setIntermediateState(flippedState);
  }, flipStateEventConsumer -> (intermediate, original) -> {
    final FlipStateEvent flipStateEvent = new FlipStateEvent(original);
    flipStateEventConsumer.accept(flipStateEvent);
    return flipStateEvent.getIntermediateState();
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
  public EventBridge<FlipStateCallback> flipState() {
    return FLIP_STATE;
  }

}
