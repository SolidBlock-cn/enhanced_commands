package pers.solid.ecmd.api.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import pers.solid.ecmd.api.ClientEventBridges;
import pers.solid.ecmd.api.EventBridge;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
@MethodsReturnNonnullByDefault
public enum ClientEventBridgesImpl implements ClientEventBridges {
  INSTANCE;

  public static final EventBridgeImpl<Consumer<Minecraft>, ClientTickEvents.EndTick> END_CLIENT_TICK = new EventBridgeImpl<>(ClientTickEvents.END_CLIENT_TICK, minecraftConsumer -> minecraftConsumer::accept, endTick -> endTick::onEndTick);


  public static ClientEventBridges getInstance() {
    return INSTANCE;
  }

  @Override
  public EventBridge<Consumer<Minecraft>> endClientTick() {
    return END_CLIENT_TICK;
  }
}
