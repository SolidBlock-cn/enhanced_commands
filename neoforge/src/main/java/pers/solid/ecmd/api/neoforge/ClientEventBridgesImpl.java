package pers.solid.ecmd.api.neoforge;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import pers.solid.ecmd.api.ClientEventBridges;
import pers.solid.ecmd.api.EventBridge;

import java.util.function.Consumer;

/**
 * {@link ClientEventBridges} 在 NeoForge 中的实现。
 */
@OnlyIn(Dist.CLIENT)
@MethodsReturnNonnullByDefault
public enum ClientEventBridgesImpl implements ClientEventBridges {
  INSTANCE;

  public static final EventBridgeImpl.FromEventBus<Consumer<Minecraft>, ClientTickEvent.Post> END_CLIENT_TICK = new EventBridgeImpl.FromEventBus<>(ClientTickEvent.Post.class, minecraftConsumer -> post -> minecraftConsumer.accept(Minecraft.getInstance()), postConsumer -> minecraft -> postConsumer.accept(new ClientTickEvent.Post()));

  public static ClientEventBridges getInstance() {
    return INSTANCE;
  }

  @Override
  public EventBridge<Consumer<Minecraft>> endClientTick() {
    return END_CLIENT_TICK;
  }
}
