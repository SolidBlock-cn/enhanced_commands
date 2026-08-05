package pers.solid.ecmd.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import static pers.solid.ecmd.tag.EnhancedCommandsBlockTags.*;

public class BlockTagDataGeneration implements TagGenerationBridge<Block> {
  @Override
  public void configure(TagBuilderFactoryBridge<Block> bridge, HolderLookup.Provider registries) {
    bridge.builderBridgeOf(OVERLAID_DIRT).add(Blocks.GRASS_BLOCK, Blocks.MYCELIUM);
    bridge.builderBridgeOf(DEAD_CORAL_BLOCKS).add(Blocks.DEAD_BRAIN_CORAL_BLOCK, Blocks.DEAD_BUBBLE_CORAL_BLOCK, Blocks.DEAD_FIRE_CORAL_BLOCK, Blocks.DEAD_HORN_CORAL_BLOCK, Blocks.DEAD_TUBE_CORAL_BLOCK);

    bridge.builderBridgeOf(NATURALIZE_IGNORED).addTag(BlockTags.REPLACEABLE).addTag(BlockTags.LEAVES).addTag(BlockTags.WART_BLOCKS).addTag(BlockTags.LOGS).addTag(SharedCommonTags.buds()).addTag(SharedCommonTags.villagerJobSites()).add(Blocks.GLOWSTONE, Blocks.COBWEB, Blocks.SHROOMLIGHT, Blocks.BONE_BLOCK);

    bridge.builderBridgeOf(NETHER_FUNGUS).add(Blocks.CRIMSON_FUNGUS, Blocks.WARPED_FUNGUS);
    bridge.builderBridgeOf(NETHER_ROOTS).add(Blocks.CRIMSON_ROOTS, Blocks.WARPED_ROOTS);
    bridge.builderBridgeOf(NETHER_NATURAL_STEM).add(Blocks.CRIMSON_STEM, Blocks.WARPED_STEM);
    bridge.builderBridgeOf(NETHER_NATURAL_HYPHAE).add(Blocks.CRIMSON_HYPHAE, Blocks.WARPED_HYPHAE);
    bridge.builderBridgeOf(NETHER_STRIPPED_STEM).add(Blocks.STRIPPED_CRIMSON_STEM, Blocks.STRIPPED_WARPED_STEM);
    bridge.builderBridgeOf(NETHER_STRIPPED_HYPHAE).add(Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE);
    bridge.builderBridgeOf(NETHER_VINES).add(Blocks.TWISTING_VINES, Blocks.WEEPING_VINES, Blocks.TWISTING_VINES_PLANT, Blocks.WEEPING_VINES_PLANT);
  }
}
