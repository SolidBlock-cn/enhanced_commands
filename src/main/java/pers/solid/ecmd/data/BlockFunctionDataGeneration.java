package pers.solid.ecmd.data;

import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.function.block.*;
import pers.solid.ecmd.function.property.AllOriginalPropertyNameFunctions;
import pers.solid.ecmd.function.property.AllRandomPropertyNameFunction;
import pers.solid.ecmd.function.property.SimplePropertyFunction;
import pers.solid.ecmd.math.Checkerboard;
import pers.solid.ecmd.math.Noise;
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
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class BlockFunctionDataGeneration extends FabricDynamicRegistryProvider {
  public BlockFunctionDataGeneration(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, registriesFuture);
  }

  protected static ResourceKey<BlockFunction> of(String value) {
    return ResourceKey.create(BlockFunction.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  protected static WeightedList.Uniform<BlockFunction> uniformSimple(Block... blocks) {
    return new WeightedList.Uniform<>(Arrays.stream(blocks).<BlockFunction>map(SimpleBlockFunction::new).toList());
  }

  @Override
  protected void configure(HolderLookup.Provider registries, Entries entries) {
    entries.add(of("typical_checkerboard"), new CheckerboardBlockFunction(uniformSimple(Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE)));
    entries.add(of("typical_checkerboard_large"), new CheckerboardBlockFunction(uniformSimple(Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE), new Vec3(4, 4, 4), Checkerboard.UNIT, Checkerboard.UNIT));
    entries.add(of("typical_checkerboard_strip"), new CheckerboardBlockFunction(uniformSimple(Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE), Checkerboard.UNIT, new Vec3(0.25f, 0.25f, 0.25f), Vec3.ZERO));
    entries.add(of("rainbow_checkerboard"), new CheckerboardBlockFunction(uniformSimple(Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.MAGENTA_CONCRETE)));
    entries.add(of("rainbow_checkerboard_large"), new CheckerboardBlockFunction(uniformSimple(Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.MAGENTA_CONCRETE), Checkerboard.UNIT, new Vec3(1 / 3f, 1 / 3f, 1 / 3f), Vec3.ZERO));
    entries.add(of("typical_noise"), new NoiseBlockFunction(uniformSimple(Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), Noise.UNIT, Vec3.ZERO));
    entries.add(of("typical_noise_large"), new NoiseBlockFunction(uniformSimple(Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.25f, 0.25f, 0.25f), Vec3.ZERO));
    entries.add(of("rainbow_noise"), new NoiseBlockFunction(uniformSimple(Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.MAGENTA_CONCRETE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.25f, 0.25f, 0.25f), Vec3.ZERO));
    entries.add(of("rainbow_noise_large"), new NoiseBlockFunction(uniformSimple(Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.MAGENTA_CONCRETE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-2, Noise.DEFAULT_AMPLITUDES), new Vec3(0.125f, 0.125f, 0.125f), Vec3.ZERO));

    final var natualizeIgnore = new TagBlockPredicate(ModBlockTags.NATUALIZE_IGNORE);
    final HolderLookup.RegistryLookup<Block> wrapper = registries.lookupOrThrow(Registries.BLOCK);
    entries.add(of("naturalize_vegetation"), new ConditionsBlockFunction(
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
        )));
    entries.add(of("overworld_plains"), new OverlayBlockFunction(new ReferenceBlockFunction(of("naturalize_vegetation")),
        new ConditionsBlockFunction(
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
        )));
    entries.add(of("overworld_mushroom"), new OverlayBlockFunction(new ReferenceBlockFunction(of("naturalize_vegetation")),
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(
                natualizeIgnore,
                EmptyBlockFunction.INSTANCE
            ),
            new ConditionalBlockFunction(
                new HorizontalOffsetBlockPredicate(1, natualizeIgnore),
                new SimpleBlockFunction(Blocks.MYCELIUM)),
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
        )));
    entries.add(of("overworld_desert"),
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(
                natualizeIgnore,
                EmptyBlockFunction.INSTANCE
            ),
            new ConditionalBlockFunction(
                new HorizontalOffsetBlockPredicate(2, natualizeIgnore),
                new SimpleBlockFunction(Blocks.SAND)),
            new ConditionalBlockFunction(
                new AnyBlockPredicate(
                    new HorizontalOffsetBlockPredicate(3, natualizeIgnore),
                    new HorizontalOffsetBlockPredicate(4, natualizeIgnore),
                    new HorizontalOffsetBlockPredicate(5, natualizeIgnore)),
                new SimpleBlockFunction(Blocks.SANDSTONE)),
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
                    Vec3.ZERO,
                    new Vec3(1, 0, 0),
                    Vec3.ZERO
                )),
                Vec3.ZERO,
                new Vec3(0, 1, 0),
                Vec3.ZERO
            )),
        Vec3.ZERO,
        new Vec3(0, 0, 1),
        Vec3.ZERO
    ));
    entries.add(of("any_dried_block"), new DryBlockFunction(RandomBlockFunction.RANDOM_SEED));
    entries.add(of("white_gray_stone_checker"), new CheckerboardBlockFunction(new WeightedList.Uniform<>(
        new ReferenceBlockFunction(of("white_colors")),
        new ReferenceBlockFunction(of("gray_colors"))
    ), Vec3.ZERO, new Vec3(3, 3, 3), Vec3.ZERO));
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
                new AnyBlockPredicate(new SimpleBlockPredicate(Blocks.SHORT_GRASS), new SimpleBlockPredicate(Blocks.FERN), new SimpleBlockPredicate(Blocks.TALL_GRASS, List.of(new ComparisonPropertyPredicate<>(BlockStateProperties.DOUBLE_BLOCK_HALF, Comparator.EQ, DoubleBlockHalf.LOWER)))),
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
                new AnyBlockPredicate(new SimpleBlockPredicate(Blocks.SHORT_GRASS), new SimpleBlockPredicate(Blocks.FERN), new SimpleBlockPredicate(Blocks.TALL_GRASS, List.of(new ComparisonPropertyPredicate<>(BlockStateProperties.DOUBLE_BLOCK_HALF, Comparator.EQ, DoubleBlockHalf.LOWER)))),
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
  public @NotNull String getName() {
    return "Block Functions";
  }
}
