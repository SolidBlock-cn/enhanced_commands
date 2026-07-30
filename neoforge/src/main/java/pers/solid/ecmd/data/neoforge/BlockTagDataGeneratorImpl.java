package pers.solid.ecmd.data.neoforge;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;

import java.util.concurrent.CompletableFuture;

import static pers.solid.ecmd.tag.EnhancedCommandsBlockTags.*;

public class BlockTagDataGeneratorImpl extends BlockTagsProvider {
  public BlockTagDataGeneratorImpl(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
    super(output, lookupProvider, EnhancedCommands.MOD_ID, existingFileHelper);
  }

  @Override
  protected void addTags(HolderLookup.Provider provider) {
    tag(RED_COLORS).add(Blocks.RED_WOOL, Blocks.RED_CONCRETE, Blocks.REDSTONE_BLOCK, Blocks.NETHER_WART_BLOCK);
    tag(ORANGE_COLORS).add(Blocks.ORANGE_WOOL, Blocks.ORANGE_CONCRETE, Blocks.PUMPKIN);
    tag(YELLOW_COLORS).add(Blocks.YELLOW_WOOL, Blocks.YELLOW_CONCRETE, Blocks.YELLOW_TERRACOTTA, Blocks.GOLD_BLOCK);
    tag(GREEN_COLORS).add(Blocks.MELON, Blocks.LIME_WOOL, Blocks.GREEN_WOOL, Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.LIME_TERRACOTTA, Blocks.MOSS_BLOCK);
    tag(LIGHT_BLUE_COLORS).add(Blocks.LIGHT_BLUE_CONCRETE, Blocks.LIGHT_BLUE_WOOL, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.DIAMOND_BLOCK);
    tag(BLUE_COLORS).add(Blocks.BLUE_WOOL, Blocks.BLUE_CONCRETE, Blocks.LAPIS_BLOCK);
    tag(PINK_COLORS).add(Blocks.PINK_WOOL, Blocks.PINK_CONCRETE, Blocks.STRIPPED_CHERRY_WOOD);
    tag(BLACK_COLORS).add(Blocks.BLACK_WOOL, Blocks.BLACK_CONCRETE, Blocks.BLACK_TERRACOTTA, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE);
    tag(GRAY_COLORS).add(Blocks.STONE, Blocks.ANDESITE, Blocks.TUFF);
    tag(WHITE_COLORS).add(Blocks.CALCITE, Blocks.WHITE_WOOL, Blocks.WHITE_CONCRETE, Blocks.SMOOTH_QUARTZ);

    tag(OVERLAID_DIRT).add(Blocks.GRASS_BLOCK, Blocks.MYCELIUM);
    tag(DEAD_CORAL_BLOCK).add(Blocks.DEAD_BRAIN_CORAL_BLOCK, Blocks.DEAD_BUBBLE_CORAL_BLOCK, Blocks.DEAD_FIRE_CORAL_BLOCK, Blocks.DEAD_HORN_CORAL_BLOCK, Blocks.DEAD_TUBE_CORAL_BLOCK);

    tag(NATUALIZE_IGNORE).addTag(BlockTags.REPLACEABLE).addTag(BlockTags.LEAVES).addTag(BlockTags.WART_BLOCKS).addTag(BlockTags.LOGS).addTag(Tags.Blocks.BUDS).addTag(Tags.Blocks.VILLAGER_JOB_SITES).add(Blocks.GLOWSTONE, Blocks.COBWEB, Blocks.SHROOMLIGHT, Blocks.BONE_BLOCK);

    tag(NETHER_FUNGUS).add(Blocks.CRIMSON_FUNGUS, Blocks.WARPED_FUNGUS);
    tag(NETHER_ROOTS).add(Blocks.CRIMSON_ROOTS, Blocks.WARPED_ROOTS);
    tag(NETHER_NATURAL_STEM).add(Blocks.CRIMSON_STEM, Blocks.WARPED_STEM);
    tag(NETHER_NATURAL_HYPHAE).add(Blocks.CRIMSON_HYPHAE, Blocks.WARPED_HYPHAE);
    tag(NETHER_STRIPPED_STEM).add(Blocks.STRIPPED_CRIMSON_STEM, Blocks.STRIPPED_WARPED_STEM);
    tag(NETHER_STRIPPED_HYPHAE).add(Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE);
    tag(NETHER_VINES).add(Blocks.TWISTING_VINES, Blocks.WEEPING_VINES, Blocks.TWISTING_VINES_PLANT, Blocks.WEEPING_VINES_PLANT);
  }
}
