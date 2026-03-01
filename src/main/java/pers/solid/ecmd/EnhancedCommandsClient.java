package pers.solid.ecmd;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import pers.solid.ecmd.api.ClientEventBridges;
import pers.solid.ecmd.config.CommandsConfig;
import pers.solid.ecmd.util.extension.BlockableEventLoopExtension;

@Environment(EnvType.CLIENT)
public class EnhancedCommandsClient {
  private static boolean initialized = false;

  public static void init() {
    if (initialized) {
      throw new IllegalStateException("EnhancedCommandsClient: Cannot initialize twice");
    }
    // 注册客户端运行任务的事件
    ClientEventBridges.INSTANCE.endClientTick().register(EnhancedCommands.id("tick_iterator_task"), client -> {
      client.getProfiler().push("enhanced_commands:tick_iterator_task");
      ((BlockableEventLoopExtension) client).ec_advanceTasks();
      client.getProfiler().pop();
    });

    // experimental: draw outline
    registerDebugRenderEvent();

    // experimental: debug render layer command
    if (CommandsConfig.current.enableDebugCommands) {
      registerClientCommands();
    }

    initialized = true;
  }

  @ExpectPlatform
  private static void registerDebugRenderEvent() {
    throw new AssertionError();
  }

  @ExpectPlatform
  private static void registerClientCommands() {
    throw new AssertionError();
  }
}
