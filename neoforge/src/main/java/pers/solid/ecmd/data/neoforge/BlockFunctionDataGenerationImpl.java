package pers.solid.ecmd.data.neoforge;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.data.BlockFunctionDataGeneration;
import pers.solid.ecmd.function.block.BlockFunction;

import java.util.Set;

@EventBusSubscriber(modid = EnhancedCommands.MOD_ID)
public class BlockFunctionDataGenerationImpl implements BlockFunctionDataGeneration, DynamicRegistryGenerationBridgeImpl<BlockFunction> {
  @SubscribeEvent
  public static void gatherData(GatherDataEvent.Server event) {
    event.createDatapackRegistryObjects(new RegistrySetBuilder().add(BlockFunction.REGISTRY_KEY, new BlockFunctionDataGenerationImpl()::configureBridge),
        Set.of(EnhancedCommands.MOD_ID));
  }

  public static TagKey<Block> oresConventionalTag() {
    return Tags.Blocks.ORES;
  }
}
