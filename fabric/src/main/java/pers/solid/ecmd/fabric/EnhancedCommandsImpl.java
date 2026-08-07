package pers.solid.ecmd.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.fabric.InitializeContextImpl;
import pers.solid.ecmd.command.EnhancedCommandsCommands;

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

    ResourceManagerHelper.registerBuiltinResourcePack(EnhancedCommands.id("examples"), FabricLoader.getInstance().getModContainer(EnhancedCommands.MOD_ID).orElseThrow(), Component.translatable("enhanced_commands.pack.examples"), ResourcePackActivationType.NORMAL);
  }
}
