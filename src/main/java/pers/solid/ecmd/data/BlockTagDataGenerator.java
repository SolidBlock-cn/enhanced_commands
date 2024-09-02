package pers.solid.ecmd.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

import static pers.solid.ecmd.tag.ModBlockTags.*;

public class BlockTagDataGenerator extends FabricTagProvider.BlockTagProvider {
  public BlockTagDataGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
    super(output, registriesFuture);
  }

  @Override
  protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
    getOrCreateTagBuilder(RED_COLORS).add(Blocks.RED_WOOL, Blocks.RED_CONCRETE, Blocks.REDSTONE_BLOCK, Blocks.NETHER_WART_BLOCK);
    getOrCreateTagBuilder(ORANGE_COLORS).add(Blocks.ORANGE_WOOL, Blocks.ORANGE_CONCRETE, Blocks.PUMPKIN);
    getOrCreateTagBuilder(YELLOW_COLORS).add(Blocks.YELLOW_WOOL, Blocks.YELLOW_CONCRETE, Blocks.YELLOW_TERRACOTTA, Blocks.GOLD_BLOCK);
    getOrCreateTagBuilder(GREEN_COLORS).add(Blocks.MELON, Blocks.LIME_WOOL, Blocks.GREEN_WOOL, Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.LIME_TERRACOTTA, Blocks.MOSS_BLOCK);
    getOrCreateTagBuilder(LIGHT_BLUE_COLORS).add(Blocks.LIGHT_BLUE_CONCRETE, Blocks.LIGHT_BLUE_WOOL, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.DIAMOND_BLOCK);
    getOrCreateTagBuilder(BLUE_COLORS).add(Blocks.BLUE_WOOL, Blocks.BLUE_CONCRETE, Blocks.LAPIS_BLOCK);
    getOrCreateTagBuilder(PINK_COLORS).add(Blocks.PINK_WOOL, Blocks.PINK_CONCRETE, Blocks.STRIPPED_CHERRY_WOOD);
    getOrCreateTagBuilder(BLACK_COLORS).add(Blocks.BLACK_WOOL, Blocks.BLACK_CONCRETE, Blocks.BLACK_TERRACOTTA, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE);
    getOrCreateTagBuilder(GRAY_COLORS).add(Blocks.STONE, Blocks.ANDESITE, Blocks.TUFF);
    getOrCreateTagBuilder(WHITE_COLORS).add(Blocks.CALCITE, Blocks.WHITE_WOOL, Blocks.WHITE_CONCRETE, Blocks.SMOOTH_QUARTZ);

    getOrCreateTagBuilder(OVERLAID_DIRT).add(Blocks.GRASS_BLOCK, Blocks.MYCELIUM);
    getOrCreateTagBuilder(DEAD_CORAL_BLOCK).add(Blocks.DEAD_BRAIN_CORAL_BLOCK, Blocks.DEAD_BUBBLE_CORAL_BLOCK, Blocks.DEAD_FIRE_CORAL_BLOCK, Blocks.DEAD_HORN_CORAL_BLOCK, Blocks.DEAD_TUBE_CORAL_BLOCK);

    getOrCreateTagBuilder(NATUALIZE_IGNORE).forceAddTag(BlockTags.REPLACEABLE).forceAddTag(BlockTags.LEAVES).forceAddTag(BlockTags.WART_BLOCKS).forceAddTag(BlockTags.LOGS).forceAddTag(ConventionalBlockTags.BUDS).forceAddTag(ConventionalBlockTags.VILLAGER_JOB_SITES).add(Blocks.GLOWSTONE, Blocks.COBWEB, Blocks.SHROOMLIGHT, Blocks.BONE_BLOCK);

    getOrCreateTagBuilder(NETHER_FUNGUS).add(Blocks.CRIMSON_FUNGUS, Blocks.WARPED_FUNGUS);
    getOrCreateTagBuilder(NETHER_ROOTS).add(Blocks.CRIMSON_ROOTS, Blocks.WARPED_ROOTS);
    getOrCreateTagBuilder(NETHER_NATURAL_STEM).add(Blocks.CRIMSON_STEM, Blocks.WARPED_STEM);
    getOrCreateTagBuilder(NETHER_NATURAL_HYPHAE).add(Blocks.CRIMSON_HYPHAE, Blocks.WARPED_HYPHAE);
    getOrCreateTagBuilder(NETHER_STRIPPED_STEM).add(Blocks.STRIPPED_CRIMSON_STEM, Blocks.STRIPPED_WARPED_STEM);
    getOrCreateTagBuilder(NETHER_STRIPPED_HYPHAE).add(Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE);
    getOrCreateTagBuilder(NETHER_VINES).add(Blocks.TWISTING_VINES, Blocks.WEEPING_VINES, Blocks.TWISTING_VINES_PLANT, Blocks.WEEPING_VINES_PLANT);
  }
}
