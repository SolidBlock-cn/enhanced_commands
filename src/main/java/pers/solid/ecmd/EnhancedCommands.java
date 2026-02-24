package pers.solid.ecmd;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.ecmd.api.FlipStateCallback;
import pers.solid.ecmd.argument.ModArgumentTypes;
import pers.solid.ecmd.command.ModCommands;
import pers.solid.ecmd.config.ConfigCategories;
import pers.solid.ecmd.config.ConfigCategory;
import pers.solid.ecmd.config.ConfigManager;
import pers.solid.ecmd.curve.CurveTypes;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.BlockFunctionTypes;
import pers.solid.ecmd.function.nbt.NbtFunctionTypes;
import pers.solid.ecmd.mixins.ext.BlockableEventLoopExtension;
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

public class EnhancedCommands implements ModInitializer {
  public static final String MOD_ID = "enhanced_commands";
  public static final Logger LOGGER = LoggerFactory.getLogger(EnhancedCommands.class);

  private static final ResourceLocation EXAMPLE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "");

  public static ResourceLocation id(String path) {
    return EXAMPLE_ID.withPath(path);
  }

  @Override
  public void onInitialize() {
    ConfigCategories.init();
    ConfigManager.loadAllConfigsFromJson();

    BlockPredicateTypes.init();
    BlockFunctionTypes.init();
    CommandEnumType.init();
    CurveTypes.init();
    EntityPredicateTypes.init();
    NbtDataRegistry.init();
    NbtFunctionTypes.init();
    NbtPredicateTypes.init();
    RegionTypes.init();
    RegionSelectionTypes.init();
    ModArgumentTypes.init();
    EntitySelectorOptionsExtension.init();
    ModTrackedData.init();

    // 注册命令
    CommandRegistrationCallback.EVENT.register(ModCommands.INSTANCE);
    FlipStateCallback.registerDefaultEvent();
    WandEvent.registerEvents();

    // 注册服务器运行任务的事件
    ServerTickEvents.END_SERVER_TICK.register(id("tick_iterator_task"), server -> {
      server.getProfiler().push("enhanced_commands:tick_iterator_task");
      ((BlockableEventLoopExtension) server).ec_advanceTasks();
      server.getProfiler().pop();
    });
    ServerLifecycleEvents.AFTER_SAVE.register(id("save_config"), (server, flush, force) -> {
      for (ConfigCategory<?> category : ConfigCategory.REGISTRY.values()) {
        if (category.isDirty()) {
          ConfigManager.saveCategoryToFile(category);
        }
      }
    });

    // 资源包
    EnhancedReloadableRegistries.register(BlockFunction.REGISTRY_KEY, BlockFunction.CODEC);
    EnhancedReloadableRegistries.register(BlockPredicate.REGISTRY_KEY, BlockPredicate.CODEC);
    EnhancedReloadableRegistries.register(Region.REGISTRY_KEY, Region.CODEC);
  }
}
