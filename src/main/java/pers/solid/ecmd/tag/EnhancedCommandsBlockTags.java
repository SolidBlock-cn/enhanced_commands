package pers.solid.ecmd.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import pers.solid.ecmd.EnhancedCommands;

/**
 * @see net.minecraft.tags.BlockTags
 */
public final class EnhancedCommandsBlockTags {
  public static final TagKey<Block> OVERLAID_DIRT = of("overlaid_dirt");
  public static final TagKey<Block> DEAD_CORAL_BLOCKS = of("dead_coral_blocks");

  public static final TagKey<Block> NATURALIZE_IGNORED = of("naturalize_ignored");

  public static final TagKey<Block> NETHER_FUNGUS = of("nether_fungus");
  public static final TagKey<Block> NETHER_ROOTS = of("nether_roots");
  public static final TagKey<Block> NETHER_NATURAL_STEM = of("nether_natural_stem");
  public static final TagKey<Block> NETHER_NATURAL_HYPHAE = of("nether_natural_hyphae");
  public static final TagKey<Block> NETHER_STRIPPED_STEM = of("nether_stripped_stem");
  public static final TagKey<Block> NETHER_STRIPPED_HYPHAE = of("nether_stripped_hyphae");
  public static final TagKey<Block> NETHER_VINES = of("nether_vines");

  private static TagKey<Block> of(String name) {
    return TagKey.create(Registries.BLOCK, EnhancedCommands.id(name));
  }
}
