package pers.solid.ecmd.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.EventBridge;
import pers.solid.ecmd.api.fabric.EventBridgeImpl;
import pers.solid.ecmd.command.ModCommands;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

public class EnhancedCommandsImpl implements ModInitializer {
  public static void registerModCommands() {
    CommandRegistrationCallback.EVENT.register(ModCommands.INSTANCE::register);
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

  public static <T> EventBridge<T> create(Class<? super T> type, Function<T[], T> invokerFactory) {
    return new EventBridgeImpl<>(EventFactory.createArrayBacked(type, invokerFactory), Function.identity(), Function.identity());
  }

  @Override
  public void onInitialize() {
    EnhancedCommands.init(new InitializeContextImpl());
  }
}
