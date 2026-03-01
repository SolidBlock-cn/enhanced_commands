package pers.solid.ecmd.api.fabric;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.MethodsReturnNonnullByDefault;
import pers.solid.ecmd.api.EventBridge;
import pers.solid.ecmd.api.EventBridges;

@MethodsReturnNonnullByDefault
public enum EventBridgesImpl implements EventBridges {
  INSTANCE;
  public static final EventBridgeImpl<UseBlockCallbackBridge, UseBlockCallback> USE_BLOCK = new EventBridgeImpl<>(UseBlockCallback.EVENT, useBlockCallbackBridge -> useBlockCallbackBridge::interact, useBlockCallback -> useBlockCallback::interact);
  public static final EventBridgeImpl<AttackBlockCallbackBridge, AttackBlockCallback> ATTACK_BLOCK = new EventBridgeImpl<>(AttackBlockCallback.EVENT, attackBlockCallbackBridge -> attackBlockCallbackBridge::interact, attackBlockCallback -> attackBlockCallback::interact);

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
