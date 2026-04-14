package pers.solid.ecmd.data.neoforge;

import net.minecraft.core.RegistrySetBuilder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.block.predicate.BlockPredicate;

import java.util.Set;

@EventBusSubscriber(modid = EnhancedCommands.MOD_ID)
public class EnhancedCommandsDataGenerationImpl {
  @SubscribeEvent
  public static void gatherData(GatherDataEvent.Client event) {
    event.createProvider(BlockTagDataGeneratorImpl::new);

    event.createDatapackRegistryObjects(new RegistrySetBuilder()
            .add(BlockFunction.REGISTRY_KEY, new BlockFunctionDataGenerationImpl()::configureBridge)
            .add(BlockPredicate.REGISTRY_KEY, new BlockPredicateDataGenerationImpl()::configureBridge),
        Set.of(EnhancedCommands.MOD_ID));

  }
}
