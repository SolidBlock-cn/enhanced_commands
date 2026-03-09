package pers.solid.ecmd.data.neoforge;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.predicate.block.BlockPredicate;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = EnhancedCommands.MOD_ID)
public class EnhancedCommandsDataGenerationImpl {
  @SubscribeEvent
  public static void gatherData(GatherDataEvent event) {
    PackOutput output = event.getGenerator().getPackOutput();
    CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
    ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
    event.getGenerator().addProvider(event.includeServer(), new BlockTagDataGeneratorImpl(output, lookupProvider, existingFileHelper));

    event.createDatapackRegistryObjects(new RegistrySetBuilder()
            .add(BlockFunction.REGISTRY_KEY, new BlockFunctionDataGenerationImpl()::configureBridge)
            .add(BlockPredicate.REGISTRY_KEY, new BlockPredicateDataGenerationImpl()::configureBridge),
        Set.of(EnhancedCommands.MOD_ID));

  }
}
