package pers.solid.ecmd.api;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

/**
 * 本模组中的客户端专用的 {@link EventBridge}。
 *
 * @see #INSTANCE
 */
@Environment(EnvType.CLIENT)
@MethodsReturnNonnullByDefault
public interface ClientEventBridges {
  ClientEventBridges INSTANCE = getInstance();

  /**
   * @see #INSTANCE
   */
  @ExpectPlatform
  static ClientEventBridges getInstance() {
    throw new AssertionError();
  }

  EventBridge<Consumer<Minecraft>> endClientTick();
}
