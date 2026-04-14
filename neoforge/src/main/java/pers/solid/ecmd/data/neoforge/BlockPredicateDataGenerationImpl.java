package pers.solid.ecmd.data.neoforge;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.data.BlockPredicateDataGeneration;

public class BlockPredicateDataGenerationImpl implements BlockPredicateDataGeneration, DynamicRegistryGenerationBridgeImpl<BlockPredicate> {
  public static TagKey<Block> conventionalBudsTag() {
    return Tags.Blocks.BUDS;
  }
}
