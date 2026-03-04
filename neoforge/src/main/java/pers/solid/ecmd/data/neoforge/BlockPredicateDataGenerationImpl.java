package pers.solid.ecmd.data.neoforge;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.data.BlockPredicateDataGeneration;
import pers.solid.ecmd.predicate.block.BlockPredicate;

import java.util.Set;

@EventBusSubscriber(modid = EnhancedCommands.MOD_ID)
public class BlockPredicateDataGenerationImpl implements BlockPredicateDataGeneration, DynamicRegistryGenerationBridgeImpl<BlockPredicate> {
  static TagKey<Block> conventionalBudsTag() {
    return Tags.Blocks.BUDS;
  }

  @SubscribeEvent
  public static void gatherData(GatherDataEvent.Server event) {
    event.createDatapackRegistryObjects(new RegistrySetBuilder().add(BlockPredicate.REGISTRY_KEY, new BlockPredicateDataGenerationImpl()::configureBridge),
        Set.of(EnhancedCommands.MOD_ID));
  }
}
