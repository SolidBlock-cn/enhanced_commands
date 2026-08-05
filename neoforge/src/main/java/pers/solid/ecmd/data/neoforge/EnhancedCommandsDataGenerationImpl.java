package pers.solid.ecmd.data.neoforge;

import net.minecraft.core.RegistrySetBuilder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.data.*;
import pers.solid.ecmd.item.function.ItemFunction;
import pers.solid.ecmd.item.predicate.ItemPredicate;

import java.util.Set;

@EventBusSubscriber(modid = EnhancedCommands.MOD_ID)
public class EnhancedCommandsDataGenerationImpl {
  @SubscribeEvent
  public static void gatherData(GatherDataEvent.Client event) {
    event.createProvider(BlockTagDataGeneratorImpl::new);

    event.createDatapackRegistryObjects(new RegistrySetBuilder()
            .add(BlockFunction.REGISTRY_KEY, registryBootstrapFor(new BlockFunctionDataGeneration()))
            .add(BlockPredicate.REGISTRY_KEY, registryBootstrapFor(new BlockPredicateDataGeneration()))
            .add(ItemFunction.REGISTRY_KEY, registryBootstrapFor(new ItemFunctionDataGeneration()))
            .add(ItemPredicate.REGISTRY_KEY, registryBootstrapFor(new ItemPredicateDataGeneration())),
        Set.of(EnhancedCommands.MOD_ID));
  }

  private static <T> RegistrySetBuilder.RegistryBootstrap<T> registryBootstrapFor(DynamicRegistryGenerationBridge<T> bridge) {
    return arg -> bridge.configureBridge(new DynamicRegistryGenerationBridgeImpl.NeoForgeContext<>(arg));
  }
}
