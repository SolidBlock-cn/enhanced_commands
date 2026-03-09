package pers.solid.ecmd.api.fabric;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.MethodsReturnNonnullByDefault;
import pers.solid.ecmd.api.EventBridge;
import pers.solid.ecmd.api.EventBridges;
import pers.solid.ecmd.api.FlipStateCallback;

/**
 * 模组中的 {@link EventBridges} 在 Fabric 中的实现。
 */
@MethodsReturnNonnullByDefault
public enum EventBridgesImpl implements EventBridges {
  INSTANCE;
  public static final EventBridgeImpl<UseBlockCallbackBridge> USE_BLOCK = new EventBridgeImpl.FromFabricEvent<>(UseBlockCallback.EVENT, useBlockCallbackBridge -> useBlockCallbackBridge::interact, useBlockCallback -> useBlockCallback::interact);

  public static final EventBridgeImpl<AttackBlockCallbackBridge> ATTACK_BLOCK = new EventBridgeImpl.FromFabricEvent<>(AttackBlockCallback.EVENT, attackBlockCallbackBridge -> attackBlockCallbackBridge::interact, attackBlockCallback -> attackBlockCallback::interact);

  public static final EventBridgeImpl<FlipStateCallback> FLIP_STATE = EventBridgeImpl.FromFabricEvent.createIdentical(EnhancedCommandsFabricEvents.FLIP_STATE);

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
