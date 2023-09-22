package pers.solid.ecmd;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;

import static pers.solid.ecmd.EnhancedCommands.MOD_ID;

@Environment(EnvType.CLIENT)
public class EnhancedCommandsClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    // 注册客户端运行任务的事件
    ClientTickEvents.END_CLIENT_TICK.register(new Identifier(MOD_ID, "tick_iterator_task"), client -> {
      client.getProfiler().push("enhanced_commands:tick_iterator_task");
      ((ThreadExecutorExtension) client).ec_advanceTasks();
      client.getProfiler().pop();
    });
  }
}
