package pers.solid.ecmd.data.neoforge;

import net.minecraft.core.RegistrySetBuilder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.data.*;
import pers.solid.ecmd.enchantment.function.EnchantmentsFunction;
import pers.solid.ecmd.entity.predicate.EntityPredicate;
import pers.solid.ecmd.item.function.ItemFunction;
import pers.solid.ecmd.item.predicate.ItemPredicate;
import pers.solid.ecmd.nbt.function.NbtFunction;
import pers.solid.ecmd.nbt.predicate.NbtPredicate;
import pers.solid.ecmd.number.EnhancedCommandsNumberProvider;
import pers.solid.ecmd.region.Region;

import java.util.Set;

@EventBusSubscriber(modid = EnhancedCommands.MOD_ID)
public class EnhancedCommandsDataGenerationImpl {
  @SubscribeEvent
  public static void gatherData(GatherDataEvent event) {
    event.createProvider((arg, completableFuture) -> new TagGenerationBridgeImpl.ForBlock(arg, completableFuture, EnhancedCommands.MOD_ID, event.getExistingFileHelper(), new BlockTagDataGeneration()));

    event.createDatapackRegistryObjects(new RegistrySetBuilder()
            .add(BlockFunction.REGISTRY_KEY, registryBootstrapFor(new BlockFunctionDataGeneration()))
            .add(BlockPredicate.REGISTRY_KEY, registryBootstrapFor(new BlockPredicateDataGeneration()))
            .add(EnchantmentsFunction.REGISTRY_KEY, registryBootstrapFor(new EnchantmentsFunctionDataGeneration()))
            .add(EntityPredicate.REGISTRY_KEY, registryBootstrapFor(new EntityPredicateDataGeneration()))
            .add(ItemFunction.REGISTRY_KEY, registryBootstrapFor(new ItemFunctionDataGeneration()))
            .add(ItemPredicate.REGISTRY_KEY, registryBootstrapFor(new ItemPredicateDataGeneration()))
            .add(NbtFunction.REGISTRY_KEY, registryBootstrapFor(new NbtFunctionDataGeneration()))
            .add(NbtPredicate.REGISTRY_KEY, registryBootstrapFor(new NbtPredicateDataGeneration()))
            .add(EnhancedCommandsNumberProvider.REGISTRY_KEY, registryBootstrapFor(new NumberProviderDataGeneration()))
            .add(Region.REGISTRY_KEY, registryBootstrapFor(new RegionDataGeneration())),
        Set.of(EnhancedCommands.MOD_ID));
  }

  private static <T> RegistrySetBuilder.RegistryBootstrap<T> registryBootstrapFor(DynamicRegistryGenerationBridge<T> bridge) {
    return arg -> bridge.configureBridge(new DynamicRegistryGenerationBridgeImpl.NeoForgeContext<>(arg));
  }
}
