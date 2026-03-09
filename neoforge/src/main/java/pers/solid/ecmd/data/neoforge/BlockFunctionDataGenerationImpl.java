package pers.solid.ecmd.data.neoforge;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import pers.solid.ecmd.data.BlockFunctionDataGeneration;
import pers.solid.ecmd.function.block.BlockFunction;

public class BlockFunctionDataGenerationImpl implements BlockFunctionDataGeneration, DynamicRegistryGenerationBridgeImpl<BlockFunction> {

  public static TagKey<Block> oresConventionalTag() {
    return Tags.Blocks.ORES;
  }
}
