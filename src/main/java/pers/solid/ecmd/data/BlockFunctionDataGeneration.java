package pers.solid.ecmd.data;

import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Vec3d;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.function.block.*;
import pers.solid.ecmd.function.property.AllOriginalPropertyNameFunctions;
import pers.solid.ecmd.function.property.AllRandomPropertyNameFunction;
import pers.solid.ecmd.function.property.SimplePropertyFunction;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.predicate.block.AnyBlockPredicate;
import pers.solid.ecmd.predicate.block.HorizontalOffsetBlockPredicate;
import pers.solid.ecmd.predicate.block.SimpleBlockPredicate;
import pers.solid.ecmd.predicate.block.TagBlockPredicate;
import pers.solid.ecmd.predicate.property.Comparator;
import pers.solid.ecmd.predicate.property.ComparisonPropertyPredicate;
import pers.solid.ecmd.tag.ModBlockTags;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class BlockFunctionDataGeneration extends FabricDynamicRegistryProvider {
  public BlockFunctionDataGeneration(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
    super(output, registriesFuture);
  }

  protected static RegistryKey<BlockFunction> of(String value) {
    return RegistryKey.of(BlockFunction.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  protected static WeightedList.Uniform<BlockFunction> uniform(Block... blocks) {
    return new WeightedList.Uniform<>(Arrays.stream(blocks).<BlockFunction>map(SimpleBlockFunction::new).toList());
  }

  @Override
  protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
    entries.add(of("black_white_wool_checkerboard"), new CheckerboardBlockFunction(uniform(Blocks.WHITE_WOOL, Blocks.BLACK_WOOL)));
    entries.add(of("pride_wool_checkerboard"), new CheckerboardBlockFunction(uniform(Blocks.RED_WOOL, Blocks.ORANGE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.BLUE_WOOL, Blocks.PURPLE_WOOL), Vec3d.ZERO, new Vec3d(2, 2, 2), Vec3d.ZERO)); // 🏳️‍🌈
    entries.add(of("trans_wool_checkerboard"), new CheckerboardBlockFunction(uniform(Blocks.LIGHT_BLUE_WOOL, Blocks.PINK_WOOL, Blocks.WHITE_WOOL, Blocks.PINK_WOOL), Vec3d.ZERO, new Vec3d(2, 2, 2), Vec3d.ZERO)); // 🏳️‍⚧️
    entries.add(of("black_white_concrete_checkerboard"), new CheckerboardBlockFunction(uniform(Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE)));
    entries.add(of("pride_concrete_checkerboard"), new CheckerboardBlockFunction(uniform(Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE), Vec3d.ZERO, new Vec3d(2, 2, 2), Vec3d.ZERO)); // 🏳️‍🌈
    entries.add(of("trans_concrete_checkerboard"), new CheckerboardBlockFunction(uniform(Blocks.LIGHT_BLUE_CONCRETE, Blocks.PINK_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.PINK_CONCRETE), Vec3d.ZERO, new Vec3d(2, 2, 2), Vec3d.ZERO)); // 🏳️‍⚧️

    final var natualizeIgnore = new TagBlockPredicate(ModBlockTags.NATUALIZE_IGNORE);
    final RegistryWrapper.Impl<Block> wrapper = registries.getWrapperOrThrow(RegistryKeys.BLOCK);
    entries.add(of("natualize"), new ConditionsBlockFunction(
        new ConditionalBlockFunction(
            new TagBlockPredicate(ModBlockTags.NETHER_FUNGUS),
            new TagBlockFunction(wrapper.getOrThrow(BlockTags.SMALL_FLOWERS))
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(ModBlockTags.NETHER_ROOTS),
            new SimpleBlockFunction(Blocks.SHORT_GRASS)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(ModBlockTags.NETHER_NATURAL_STEM),
            new SimpleBlockFunction(Blocks.OAK_LOG)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(ModBlockTags.NETHER_NATURAL_HYPHAE),
            new SimpleBlockFunction(Blocks.OAK_WOOD)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(ModBlockTags.NETHER_STRIPPED_STEM),
            new SimpleBlockFunction(Blocks.STRIPPED_OAK_LOG)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(ModBlockTags.NETHER_STRIPPED_HYPHAE),
            new SimpleBlockFunction(Blocks.STRIPPED_OAK_WOOD)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(BlockTags.WART_BLOCKS),
            new SimpleBlockFunction(Blocks.OAK_LEAVES, List.of(new SimplePropertyFunction<>(LeavesBlock.PERSISTENT, false, false)))
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(ModBlockTags.NETHER_VINES),
            new SimpleBlockFunction(Blocks.VINE)
        ),
        new ConditionalBlockFunction(
            natualizeIgnore,
            EmptyBlockFunction.INSTANCE
        ),
        new ConditionalBlockFunction(
            new HorizontalOffsetBlockPredicate(1, natualizeIgnore),
            new SimpleBlockFunction(Blocks.GRASS_BLOCK)),
        new ConditionalBlockFunction(
            new AnyBlockPredicate(
                new HorizontalOffsetBlockPredicate(2, natualizeIgnore),
                new HorizontalOffsetBlockPredicate(3, natualizeIgnore),
                new HorizontalOffsetBlockPredicate(4, natualizeIgnore)),
            new SimpleBlockFunction(Blocks.DIRT)),
        new ConditionalBlockFunction(
            new AnyBlockPredicate(new TagBlockPredicate(ConventionalBlockTags.ORES), new SimpleBlockPredicate(Blocks.STONE)),
            EmptyBlockFunction.INSTANCE,
            new SimpleBlockFunction(Blocks.STONE)
        )
    ));
    entries.add(of("air_checkerboard"), new CheckerboardBlockFunction(new WeightedList.Uniform<>(new SimpleBlockFunction(Blocks.AIR), EmptyBlockFunction.INSTANCE)));
    entries.add(of("air_grid"), new CheckerboardBlockFunction(
        new WeightedList.Uniform<>(
            new SimpleBlockFunction(Blocks.AIR),
            new CheckerboardBlockFunction(new WeightedList.Uniform<>(
                new SimpleBlockFunction(Blocks.AIR),
                new CheckerboardBlockFunction(new WeightedList.Uniform<>(
                    new SimpleBlockFunction(Blocks.AIR),
                    EmptyBlockFunction.INSTANCE
                ),
                    Vec3d.ZERO,
                    new Vec3d(1, 0, 0),
                    Vec3d.ZERO
                )),
                Vec3d.ZERO,
                new Vec3d(0, 1, 0),
                Vec3d.ZERO
            )),
        Vec3d.ZERO,
        new Vec3d(0, 0, 1),
        Vec3d.ZERO
    ));
    entries.add(of("any_dried_block"), new DryBlockFunction(new RandomBlockFunction()));
    entries.add(of("white_gray_stone_checker"), new CheckerboardBlockFunction(new WeightedList.Uniform<>(
        new ReferenceBlockFunction(of("white_colors")),
        new ReferenceBlockFunction(of("gray_colors"))
    ), Vec3d.ZERO, new Vec3d(3, 3, 3), Vec3d.ZERO));
    entries.add(of("crimsonize"), new PropertiesNbtCombinationBlockFunction(
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(new TagBlockPredicate(ModBlockTags.OVERLAID_DIRT),
                new SimpleBlockFunction(Blocks.CRIMSON_NYLIUM)),
            new ConditionalBlockFunction(
                new TagBlockPredicate(BlockTags.DIRT),
                new SimpleBlockFunction(Blocks.NETHERRACK)
            ),
            new ConditionalBlockFunction(
                new TagBlockPredicate(BlockTags.FLOWERS),
                new SimpleBlockFunction(Blocks.CRIMSON_FUNGUS)
            ),
            new ConditionalBlockFunction(
                new AnyBlockPredicate(new SimpleBlockPredicate(Blocks.SHORT_GRASS), new SimpleBlockPredicate(Blocks.FERN), new SimpleBlockPredicate(Blocks.TALL_GRASS, List.of(new ComparisonPropertyPredicate<>(Properties.DOUBLE_BLOCK_HALF, Comparator.EQ, DoubleBlockHalf.LOWER)))),
                new SimpleBlockFunction(Blocks.CRIMSON_ROOTS)
            ),
            new ConditionalBlockFunction(
                new TagBlockPredicate(BlockTags.OVERWORLD_NATURAL_LOGS),
                new SimpleBlockFunction(Blocks.CRIMSON_STEM)
            ),
            new ConditionalBlockFunction(
                new TagBlockPredicate(BlockTags.LEAVES),
                new SimpleBlockFunction(Blocks.NETHER_WART_BLOCK),
                EmptyBlockFunction.INSTANCE
            ),
            new ConditionalBlockFunction(
                new TagBlockPredicate(BlockTags.SAND),
                new SimpleBlockFunction(Blocks.SOUL_SAND)
            )
        ),
        new PropertyNamesBlockFunction(Collections.singletonList(new AllOriginalPropertyNameFunctions())),
        null
    ));
    entries.add(of("warpize"), new PropertiesNbtCombinationBlockFunction(
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(new TagBlockPredicate(ModBlockTags.OVERLAID_DIRT),
                new SimpleBlockFunction(Blocks.WARPED_NYLIUM)),
            new ConditionalBlockFunction(
                new TagBlockPredicate(BlockTags.DIRT),
                new SimpleBlockFunction(Blocks.NETHERRACK)
            ),
            new ConditionalBlockFunction(
                new TagBlockPredicate(BlockTags.FLOWERS),
                new SimpleBlockFunction(Blocks.WARPED_FUNGUS)
            ),
            new ConditionalBlockFunction(
                new AnyBlockPredicate(new SimpleBlockPredicate(Blocks.SHORT_GRASS), new SimpleBlockPredicate(Blocks.FERN), new SimpleBlockPredicate(Blocks.TALL_GRASS, List.of(new ComparisonPropertyPredicate<>(Properties.DOUBLE_BLOCK_HALF, Comparator.EQ, DoubleBlockHalf.LOWER)))),
                new SimpleBlockFunction(Blocks.WARPED_ROOTS)
            ),
            new ConditionalBlockFunction(
                new TagBlockPredicate(BlockTags.OVERWORLD_NATURAL_LOGS),
                new SimpleBlockFunction(Blocks.WARPED_STEM)
            ),
            new ConditionalBlockFunction(
                new TagBlockPredicate(BlockTags.LEAVES),
                new SimpleBlockFunction(Blocks.WARPED_WART_BLOCK),
                EmptyBlockFunction.INSTANCE
            ),
            new ConditionalBlockFunction(
                new TagBlockPredicate(BlockTags.SAND),
                new SimpleBlockFunction(Blocks.SOUL_SAND)
            )
        ),
        new PropertyNamesBlockFunction(Collections.singletonList(new AllOriginalPropertyNameFunctions())),
        null
    ));
    entries.add(of("concrete_to_powder"), new IdReplaceBlockFunction(Pattern.compile("_concrete$"), "_concrete_powder"));
    entries.add(of("powder_to_concrete"), new IdReplaceBlockFunction(Pattern.compile("_concrete_powder$"), "_concrete"));


    entries.add(of("red_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(ModBlockTags.RED_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    entries.add(of("orange_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(ModBlockTags.ORANGE_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    entries.add(of("yellow_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(ModBlockTags.YELLOW_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    entries.add(of("green_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(ModBlockTags.GREEN_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    entries.add(of("light_blue_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(ModBlockTags.LIGHT_BLUE_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    entries.add(of("blue_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(ModBlockTags.BLUE_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    entries.add(of("pink_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(ModBlockTags.PINK_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    entries.add(of("black_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(ModBlockTags.BLACK_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    entries.add(of("gray_colors"), new PropertiesNbtCombinationBlockFunction(new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new TagBlockFunction(wrapper.getOrThrow(ModBlockTags.GRAY_COLORS)), 0.75), ObjectDoublePair.of(new TagBlockFunction(wrapper.getOrThrow(ModBlockTags.DEAD_CORAL_BLOCK)), 0.25))), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    entries.add(of("white_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(ModBlockTags.WHITE_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    entries.add(of("slightly_worn_stone_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.STONE_BRICKS), 0.8), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_STONE_BRICKS), 0.1), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.MOSSY_STONE_BRICKS), 0.1))));
    entries.add(of("slightly_worn_nether_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.NETHER_BRICKS), 0.8), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_NETHER_BRICKS), 0.2))));
    entries.add(of("slightly_worn_deepslate_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.DEEPSLATE_BRICKS), 0.8), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_DEEPSLATE_BRICKS), 0.2))));
    entries.add(of("mediumly_worn_stone_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.STONE_BRICKS), 0.6), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_STONE_BRICKS), 0.2), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.MOSSY_STONE_BRICKS), 0.2))));
    entries.add(of("mediumly_worn_nether_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.NETHER_BRICKS), 0.6), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_NETHER_BRICKS), 0.4))));
    entries.add(of("mediumly_worn_deepslate_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.DEEPSLATE_BRICKS), 0.6), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_DEEPSLATE_BRICKS), 0.4))));
    entries.add(of("heavily_worn_stone_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.STONE_BRICKS), 0.2), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_STONE_BRICKS), 0.4), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.MOSSY_STONE_BRICKS), 0.4))));
    entries.add(of("heavily_worn_nether_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.NETHER_BRICKS), 0.2), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_NETHER_BRICKS), 0.8))));
    entries.add(of("heavily_worn_deepslate_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.DEEPSLATE_BRICKS), 0.2), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_DEEPSLATE_BRICKS), 0.8))));
  }

  @Override
  public String getName() {
    return "Block Functions";
  }
}
