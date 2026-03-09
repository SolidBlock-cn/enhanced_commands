package pers.solid.ecmd.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.command.EnhancedCommandsCommands;
import pers.solid.ecmd.registry.EnhancedDynamicRegistryInfo;
import pers.solid.ecmd.registry.EnhancedServerReloadableRegistries;

import java.nio.file.Path;
import java.util.function.Consumer;

public class EnhancedCommandsImpl implements ModInitializer {
  public static void registerModCommands() {
    CommandRegistrationCallback.EVENT.register(EnhancedCommandsCommands.INSTANCE::register);
  }

  public static boolean isDevelopmentEnvironment() {
    return FabricLoader.getInstance().isDevelopmentEnvironment();
  }

  public static Path getConfigDir() {
    return FabricLoader.getInstance().getConfigDir();
  }

  public static void registerAdvanceTasks(Consumer<MinecraftServer> consumer) {
    // 注册服务器运行任务的事件
    ServerTickEvents.END_SERVER_TICK.register(EnhancedCommands.id("tick_iterator_task"), consumer::accept);
  }

  public static void registerSaveConfig(Runnable runnable) {
    ServerLifecycleEvents.AFTER_SAVE.register(EnhancedCommands.id("save_config"), (server, flush, force) -> runnable.run());
  }


  @Override
  public void onInitialize() {
    EnhancedCommands.init(new InitializeContextImpl());

    EnhancedServerReloadableRegistries.getRegistry().values().forEach(EnhancedCommandsImpl::registerAsFabricDynamicRegistry);
  }

  /**
   * 注册到 Fabric API 的动态注册表中，以用于数据生成相关功能。
   */
  private static <T> void registerAsFabricDynamicRegistry(EnhancedDynamicRegistryInfo<T> info) {
    DynamicRegistries.register(info.registryKey(), info.codec());
  }
}
