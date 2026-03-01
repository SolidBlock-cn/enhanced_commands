package pers.solid.ecmd.api.neoforge;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import pers.solid.ecmd.api.EventBridge;
import pers.solid.ecmd.api.EventBridges;

@MethodsReturnNonnullByDefault
public enum EventBridgesImpl implements EventBridges {
  INSTANCE;
  public static final EventBridgeImpl.FromEventBus<UseBlockCallbackBridge, PlayerInteractEvent.RightClickBlock> USE_BLOCK = new EventBridgeImpl.FromEventBus<>(PlayerInteractEvent.RightClickBlock.class, useBlockCallbackBridge -> rightClickBlock -> useBlockCallbackBridge.interact(rightClickBlock.getEntity(), rightClickBlock.getLevel(), rightClickBlock.getHand(), rightClickBlock.getHitVec()), rightClickBlockConsumer -> (player, world, hand, hitResult) -> {
    final PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player, hand, hitResult.getBlockPos(), hitResult);
    rightClickBlockConsumer.accept(event);
    return event.getCancellationResult();
  });
  public static final EventBridgeImpl.FromEventBus<AttackBlockCallbackBridge, PlayerInteractEvent.LeftClickBlock> ATTACK_BLOCK = new EventBridgeImpl.FromEventBus<>(PlayerInteractEvent.LeftClickBlock.class, attackBlockCallbackBridge -> leftClickBlock -> attackBlockCallbackBridge.interact(leftClickBlock.getEntity(), leftClickBlock.getLevel(), leftClickBlock.getHand(), leftClickBlock.getPos(), leftClickBlock.getFace()), leftClickBlockConsumer -> (player, world, hand, pos, direction) -> {
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
}
