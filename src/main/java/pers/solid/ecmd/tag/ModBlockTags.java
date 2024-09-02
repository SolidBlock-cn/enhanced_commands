package pers.solid.ecmd.tag;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import pers.solid.ecmd.EnhancedCommands;

/**
 * @see net.minecraft.registry.tag.BlockTags
 */
public final class ModBlockTags {
  public static final TagKey<Block> RED_COLORS = of("red_colors");
  public static final TagKey<Block> ORANGE_COLORS = of("orange_colors");
  public static final TagKey<Block> YELLOW_COLORS = of("yellow_colors");
  public static final TagKey<Block> GREEN_COLORS = of("green_colors");
  public static final TagKey<Block> LIGHT_BLUE_COLORS = of("light_blue_colors");
  public static final TagKey<Block> BLUE_COLORS = of("blue_colors");
  public static final TagKey<Block> PINK_COLORS = of("pink_colors");
  public static final TagKey<Block> BLACK_COLORS = of("black_colors");
  public static final TagKey<Block> GRAY_COLORS = of("gray_colors");
  public static final TagKey<Block> WHITE_COLORS = of("white_colors");

  public static final TagKey<Block> OVERLAID_DIRT = of("overlaid_dirt");
  public static final TagKey<Block> DEAD_CORAL_BLOCK = of("dead_coral_block");

  public static final TagKey<Block> NATUALIZE_IGNORE = of("natualize_ignore");

  public static final TagKey<Block> NETHER_FUNGUS = of("nether_fungus");
  public static final TagKey<Block> NETHER_ROOTS = of("nether_roots");
  public static final TagKey<Block> NETHER_NATURAL_STEM = of("nether_natural_stem");
  public static final TagKey<Block> NETHER_NATURAL_HYPHAE = of("nether_natural_hyphae");
  public static final TagKey<Block> NETHER_STRIPPED_STEM = of("nether_stripped_stem");
  public static final TagKey<Block> NETHER_STRIPPED_HYPHAE = of("nether_stripped_hyphae");
  public static final TagKey<Block> NETHER_VINES = of("nether_vines");

  private static TagKey<Block> of(String name) {
    return TagKey.of(RegistryKeys.BLOCK, EnhancedCommands.id(name));
  }
}
