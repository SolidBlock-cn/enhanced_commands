package pers.solid.ecmd.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import pers.solid.ecmd.ActiveRegionRenderer;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.EnhancedCommandsClient;
import pers.solid.ecmd.render.DebugRenderLayerCommand;

@Environment(EnvType.CLIENT)
public class EnhancedCommandsClientImpl implements ClientModInitializer {
  public static void registerDebugRenderEvent() {
    WorldRenderEvents.BEFORE_DEBUG_RENDER.register(EnhancedCommands.id("active_region"), worldRenderContext -> ActiveRegionRenderer.renderActiveRegion(worldRenderContext.matrixStack(), worldRenderContext.consumers(), worldRenderContext.camera()));
  }

  public static void registerClientCommands() {
    ClientCommandRegistrationCallback.EVENT.register(EnhancedCommands.id("client_commands"), DebugRenderLayerCommand.INSTANCE::register);
  }

  @Override
  public void onInitializeClient() {
    EnhancedCommandsClient.init();
  }
}
