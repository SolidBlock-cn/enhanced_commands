package pers.solid.ecmd;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.ecmd.api.EnhancedServerReloadableRegistries;
import pers.solid.ecmd.api.FlipStateCallback;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.argument.EnhancedCommandsArgumentTypes;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.block.function.BlockFunctionTypes;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.block.predicate.BlockPredicateTypes;
import pers.solid.ecmd.config.ConfigCategories;
import pers.solid.ecmd.config.ConfigCategory;
import pers.solid.ecmd.config.ConfigManager;
import pers.solid.ecmd.curve.CurveTypes;
import pers.solid.ecmd.enchantment.function.EnchantmentModificationTypes;
import pers.solid.ecmd.entity.predicate.EntityPredicate;
import pers.solid.ecmd.entity.predicate.EntityPredicateTypes;
import pers.solid.ecmd.entity.predicate.EntitySelectorOptionsExtension;
import pers.solid.ecmd.item.function.ItemFunction;
import pers.solid.ecmd.item.function.ItemFunctionTypes;
import pers.solid.ecmd.item.predicate.ItemPredicate;
import pers.solid.ecmd.item.predicate.ItemPredicateTypes;
import pers.solid.ecmd.nbt.data.NbtDataRegistry;
import pers.solid.ecmd.nbt.function.NbtFunction;
import pers.solid.ecmd.nbt.function.NbtFunctionTypes;
import pers.solid.ecmd.nbt.predicate.NbtPredicate;
import pers.solid.ecmd.nbt.predicate.NbtPredicateTypes;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionTypes;
import pers.solid.ecmd.regionselection.RegionSelectionTypes;
import pers.solid.ecmd.regionselection.WandEvent;
import pers.solid.ecmd.util.enums.CommandEnumType;
import pers.solid.ecmd.util.extension.BlockableEventLoopExtension;

import java.nio.file.Path;
import java.util.function.Consumer;

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

    BlockFunctionTypes.init(context);
    BlockPredicateTypes.init(context);
    CommandEnumType.init(context);
    CurveTypes.init(context);
    EnchantmentModificationTypes.init(context);
    EntityPredicateTypes.init(context);
    ItemFunctionTypes.init(context);
    ItemPredicateTypes.init(context);
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

    registerReloadableRegistries(context);

    initialized = true;
  }

  /**
   * 注册此模组中的可重新加载的注册表，其加载方式类似于战利品表。
   * <p>
   * 注意：这些注册表会像其他不可重新加载的动态注册表那样，作为动态注册表注册到 Fabric API 或 NeoForge 的 API 中，但是实际加载时会被特别排除。
   *
   * @see pers.solid.ecmd.mixins.general.RegistryDataLoaderMixin
   */
  private static void registerReloadableRegistries(InitializeContext context) {
    EnhancedServerReloadableRegistries.register(BlockFunction.REGISTRY_KEY, BlockFunction.CODEC, false, context);
    EnhancedServerReloadableRegistries.register(BlockPredicate.REGISTRY_KEY, BlockPredicate.CODEC, false, context);
    EnhancedServerReloadableRegistries.register(EntityPredicate.REGISTRY_KEY, EntityPredicate.CODEC, false, context);
    EnhancedServerReloadableRegistries.register(ItemFunction.REGISTRY_KEY, ItemFunction.CODEC, false, context);
    EnhancedServerReloadableRegistries.register(ItemPredicate.REGISTRY_KEY, ItemPredicate.CODEC, false, context);
    EnhancedServerReloadableRegistries.register(NbtFunction.REGISTRY_KEY, NbtFunction.CODEC, false, context);
    EnhancedServerReloadableRegistries.register(NbtPredicate.REGISTRY_KEY, NbtPredicate.CODEC, false, context);
    EnhancedServerReloadableRegistries.register(Region.REGISTRY_KEY, Region.CODEC, false, context);
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
}
