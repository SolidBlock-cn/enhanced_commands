package pers.solid.ecmd;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import pers.solid.ecmd.config.CommandsConfig;
import pers.solid.ecmd.mixins.ext.BlockableEventLoopExtension;
import pers.solid.ecmd.render.DebugRenderLayerCommand;

@Environment(EnvType.CLIENT)
public class EnhancedCommandsClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    // 注册客户端运行任务的事件
    ClientTickEvents.END_CLIENT_TICK.register(EnhancedCommands.id("tick_iterator_task"), client -> {
      final ProfilerFiller profiler = Profiler.get();
      profiler.push("enhanced_commands:tick_iterator_task");
      ((BlockableEventLoopExtension) client).ec_advanceTasks();
      profiler.pop();
    });

    // experimental: draw outline
    WorldRenderEvents.BEFORE_DEBUG_RENDER.register(EnhancedCommands.id("active_region"), ActiveRegionRenderer.INSTANCE);

    // experimental: debug render layer command
    if (CommandsConfig.current.enableDebugCommands) {
      ClientCommandRegistrationCallback.EVENT.register(EnhancedCommands.id("client_commands"), (DebugRenderLayerCommand.INSTANCE));
    }
  }
}
