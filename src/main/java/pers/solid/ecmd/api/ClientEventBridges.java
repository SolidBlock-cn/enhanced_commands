package pers.solid.ecmd.api;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
@MethodsReturnNonnullByDefault
public interface ClientEventBridges {
  ClientEventBridges INSTANCE = getInstance();

  @ExpectPlatform
  static ClientEventBridges getInstance() {
    throw new AssertionError();
  }

  EventBridge<Consumer<Minecraft>> endClientTick();


}
