package pers.solid.ecmd.neoforge;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import org.jetbrains.annotations.ApiStatus;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.EventBridge;
import pers.solid.ecmd.api.neoforge.EventBridgeImpl;
import pers.solid.ecmd.command.ModCommands;
import pers.solid.ecmd.curve.CurveType;
import pers.solid.ecmd.function.block.BlockFunctionType;
import pers.solid.ecmd.function.nbt.NbtFunctionType;
import pers.solid.ecmd.predicate.block.BlockPredicateType;
import pers.solid.ecmd.predicate.entity.EntityPredicateType;
import pers.solid.ecmd.predicate.nbt.NbtPredicateType;
import pers.solid.ecmd.region.RegionType;
import pers.solid.ecmd.regionselection.RegionSelectionType;
import pers.solid.ecmd.util.enums.CommandEnumType;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

@Mod(EnhancedCommands.MOD_ID)
public class EnhancedCommandsImpl {
  public EnhancedCommandsImpl(IEventBus eventBus) {
    EnhancedCommands.init();

    eventBus.addListener(NewRegistryEvent.class, event -> {
      event.register(CurveType.REGISTRY);
      event.register(BlockFunctionType.REGISTRY);
      event.register(NbtFunctionType.REGISTRY);
      event.register(BlockPredicateType.REGISTRY);
      event.register(EntityPredicateType.REGISTRY);
      event.register(NbtPredicateType.REGISTRY);
      event.register(RegionType.REGISTRY);
      event.register(RegionSelectionType.REGISTRY);
      event.register(CommandEnumType.REGISTRY);
    });
  }

  @ApiStatus.Internal
  public static void registerModCommands() {
    NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> ModCommands.INSTANCE.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()));
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

  public static <T> EventBridge<T> create(Class<? super T> type, Function<T[], T> invokerFactory) {
    // todo 考虑未来改用 EventBus
    return new EventBridgeImpl.Simple<>(type, invokerFactory);
  }
}
