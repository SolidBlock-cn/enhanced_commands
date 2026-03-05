package pers.solid.ecmd;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.ecmd.api.EventBridge;
import pers.solid.ecmd.api.FlipStateCallback;
import pers.solid.ecmd.argument.EnhancedCommandsArgumentTypes;
import pers.solid.ecmd.config.ConfigCategories;
import pers.solid.ecmd.config.ConfigCategory;
import pers.solid.ecmd.config.ConfigManager;
import pers.solid.ecmd.curve.CurveTypes;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.BlockFunctionTypes;
import pers.solid.ecmd.function.nbt.NbtFunctionTypes;
import pers.solid.ecmd.nbt.NbtDataRegistry;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.BlockPredicateTypes;
import pers.solid.ecmd.predicate.entity.EntityPredicateTypes;
import pers.solid.ecmd.predicate.entity.EntitySelectorOptionsExtension;
import pers.solid.ecmd.predicate.nbt.NbtPredicateTypes;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionTypes;
import pers.solid.ecmd.regionselection.RegionSelectionTypes;
import pers.solid.ecmd.regionselection.WandEvent;
import pers.solid.ecmd.registry.EnhancedReloadableRegistries;
import pers.solid.ecmd.util.enums.CommandEnumType;
import pers.solid.ecmd.util.extension.BlockableEventLoopExtension;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

public class EnhancedCommands {
  public static final String MOD_ID = "enhanced_commands";
  public static final Logger LOGGER = LoggerFactory.getLogger(EnhancedCommands.class);

  private static final ResourceLocation EXAMPLE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "");

  public static ResourceLocation id(String path) {
    return EXAMPLE_ID.withPath(path);
  }

  private static boolean initialized = false;

  public static void init(InitializeContext context) {
    if (initialized) {
      throw new IllegalStateException("Enhanced Commands: Cannot initialize twice");
    }
    ConfigCategories.init();
    ConfigManager.loadAllConfigsFromJson();

    BlockPredicateTypes.init(context);
    BlockFunctionTypes.init(context);
    CommandEnumType.init(context);
    CurveTypes.init(context);
    EntityPredicateTypes.init(context);
    NbtDataRegistry.init();
    NbtFunctionTypes.init(context);
    NbtPredicateTypes.init(context);
    RegionTypes.init(context);
    RegionSelectionTypes.init(context);
    EnhancedCommandsArgumentTypes.init();
    EntitySelectorOptionsExtension.init();
    EnhancedCommandsDataAttachments.init();
    EnhancedCommandsTrackedData.init(context);

    // 注册命令
    registerModCommands();
    FlipStateCallback.registerDefaultEvent();
    WandEvent.registerEvents();

    registerAdvanceTasks();
    registerSaveConfig();

    // 资源包
    EnhancedReloadableRegistries.register(BlockFunction.REGISTRY_KEY, BlockFunction.CODEC);
    EnhancedReloadableRegistries.register(BlockPredicate.REGISTRY_KEY, BlockPredicate.CODEC);
    EnhancedReloadableRegistries.register(Region.REGISTRY_KEY, Region.CODEC);

    initialized = true;
  }

  @ExpectPlatform
  private static void registerModCommands() {
    throw new AssertionError();
  }

  private static void registerAdvanceTasks() {
    registerAdvanceTasks(server -> {
      server.getProfiler().push("enhanced_commands:tick_iterator_task");
      ((BlockableEventLoopExtension) server).ec_advanceTasks();
      server.getProfiler().pop();
    });
  }

  @ExpectPlatform
  private static void registerAdvanceTasks(Consumer<MinecraftServer> consumer) {
    throw new AssertionError();
  }

  private static void registerSaveConfig() {
    registerSaveConfig(() -> {
      for (ConfigCategory<?> category : ConfigCategory.REGISTRY.values()) {
        if (category.isDirty()) {
          ConfigManager.saveCategoryToFile(category);
        }
      }
    });
  }

  @ExpectPlatform
  private static void registerSaveConfig(Runnable runnable) {
    throw new AssertionError();
  }

  @ExpectPlatform
  public static boolean isDevelopmentEnvironment() {
    throw new AssertionError();
  }

  @ExpectPlatform
  public static Path getConfigDir() {
    throw new AssertionError();
  }

  @ExpectPlatform
  public static <T> EventBridge<T> create(Class<? super T> type, Function<T[], T> invokerFactory) {
    throw new AssertionError();
  }
}
