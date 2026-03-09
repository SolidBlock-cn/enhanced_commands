package pers.solid.ecmd.neoforge;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.jetbrains.annotations.ApiStatus;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.neoforge.EnhancedCommandsArgumentTypesImpl;
import pers.solid.ecmd.command.EnhancedCommandsCommands;
import pers.solid.ecmd.registry.EnhancedDynamicRegistryInfo;
import pers.solid.ecmd.registry.EnhancedServerReloadableRegistries;

import java.nio.file.Path;
import java.util.function.Consumer;

@EventBusSubscriber(modid = EnhancedCommands.MOD_ID)
@Mod(EnhancedCommands.MOD_ID)
public class EnhancedCommandsImpl {
  public EnhancedCommandsImpl(IEventBus eventBus) {
    EnhancedCommands.init(new InitializeContextImpl(eventBus));

    EnhancedCommandsDataAttachmentsImpl.DEFERRED_REGISTER.register(eventBus);
    EnhancedCommandsArgumentTypesImpl.DEFERRED_REGISTER.register(eventBus);
  }

  @ApiStatus.Internal
  public static void registerModCommands() {
    NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> EnhancedCommandsCommands.INSTANCE.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()));
  }

  @ApiStatus.Internal
  public static void registerAdvanceTasks(Consumer<MinecraftServer> consumer) {
    NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> consumer.accept(event.getServer()));
  }

  @ApiStatus.Internal
  public static void registerSaveConfig(Runnable runnable) {
    NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> {runnable.run();});
  }

  public static boolean isDevelopmentEnvironment() {
    return !FMLEnvironment.production;
  }

  public static Path getConfigDir() {
    return FMLPaths.CONFIGDIR.get();
  }


  @SubscribeEvent
  private static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
    for (EnhancedDynamicRegistryInfo<?> info : EnhancedServerReloadableRegistries.getRegistry().values()) {
      registerAsNeoForgeDataRegistry(event, info);
    }
  }

  private static <T> void registerAsNeoForgeDataRegistry(DataPackRegistryEvent.NewRegistry event, EnhancedDynamicRegistryInfo<T> info) {
    event.dataPackRegistry(info.registryKey(), info.codec());
  }
}
