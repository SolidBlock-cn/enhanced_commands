package pers.solid.ecmd.data;

import dev.architectury.injectables.annotations.ExpectPlatform;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.block.function.*;
import pers.solid.ecmd.block.predicate.AnyBlockPredicate;
import pers.solid.ecmd.block.predicate.HorizontalOffsetBlockPredicate;
import pers.solid.ecmd.block.predicate.SimpleBlockPredicate;
import pers.solid.ecmd.block.predicate.TagBlockPredicate;
import pers.solid.ecmd.math.Checkerboard;
import pers.solid.ecmd.math.Noise;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.property.function.AllOriginalPropertyNameFunctions;
import pers.solid.ecmd.property.function.AllRandomPropertyNameFunction;
import pers.solid.ecmd.property.function.SimplePropertyFunction;
import pers.solid.ecmd.property.predicate.Comparator;
import pers.solid.ecmd.property.predicate.ComparisonPropertyPredicate;
import pers.solid.ecmd.tag.EnhancedCommandsBlockTags;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.regex.Pattern;

public interface BlockFunctionDataGeneration extends DynamicRegistryGenerationBridge<BlockFunction> {
  static ResourceKey<BlockFunction> of(String value) {
    return ResourceKey.create(BlockFunction.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  static WeightedList.Uniform<BlockFunction> uniformSimple(Block... blocks) {
    return new WeightedList.Uniform<>(Arrays.stream(blocks).<BlockFunction>map(SimpleBlockFunction::new).toList());
  }

  @Override
  default void configureBridge(ContextBridge<BlockFunction> context) {
    context.add(of("black_white_checkerboard"), new CheckerboardBlockFunction(uniformSimple(Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE)));
    context.add(of("black_white_checkerboard_large"), new CheckerboardBlockFunction(uniformSimple(Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE), new Vec3(4, 4, 4), Checkerboard.UNIT, Checkerboard.UNIT));
    context.add(of("black_white_checkerboard_strip"), new CheckerboardBlockFunction(uniformSimple(Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE), Checkerboard.UNIT, new Vec3(0.25f, 0.25f, 0.25f), Vec3.ZERO));
    context.add(of("rainbow_checkerboard"), new CheckerboardBlockFunction(uniformSimple(Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.MAGENTA_CONCRETE)));
    context.add(of("rainbow_checkerboard_large"), new CheckerboardBlockFunction(uniformSimple(Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.MAGENTA_CONCRETE), Checkerboard.UNIT, new Vec3(1 / 3f, 1 / 3f, 1 / 3f), Vec3.ZERO));
    context.add(of("black_white_noise"), new NoiseBlockFunction(uniformSimple(Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), Noise.UNIT, Vec3.ZERO));
    context.add(of("black_white_noise_large"), new NoiseBlockFunction(uniformSimple(Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.25f, 0.25f, 0.25f), Vec3.ZERO));
    context.add(of("rainbow_noise"), new NoiseBlockFunction(uniformSimple(Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.MAGENTA_CONCRETE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.25f, 0.25f, 0.25f), Vec3.ZERO));
    context.add(of("rainbow_noise_large"), new NoiseBlockFunction(uniformSimple(Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.MAGENTA_CONCRETE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-2, Noise.DEFAULT_AMPLITUDES), new Vec3(0.125f, 0.125f, 0.125f), Vec3.ZERO));

    final var naturalizeIgnore = new TagBlockPredicate(EnhancedCommandsBlockTags.NATUALIZE_IGNORE);
    final HolderLookup.RegistryLookup<Block> wrapper = context.registryLookup(Registries.BLOCK).orElseThrow();
    context.add(of("naturalize_vegetation"), new ConditionsBlockFunction(
        new ConditionalBlockFunction(
            new TagBlockPredicate(EnhancedCommandsBlockTags.NETHER_FUNGUS),
            new TagBlockFunction(wrapper.getOrThrow(BlockTags.SMALL_FLOWERS))
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(EnhancedCommandsBlockTags.NETHER_ROOTS),
            new SimpleBlockFunction(Blocks.SHORT_GRASS)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(EnhancedCommandsBlockTags.NETHER_NATURAL_STEM),
            new SimpleBlockFunction(Blocks.OAK_LOG)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(EnhancedCommandsBlockTags.NETHER_NATURAL_HYPHAE),
            new SimpleBlockFunction(Blocks.OAK_WOOD)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(EnhancedCommandsBlockTags.NETHER_STRIPPED_STEM),
            new SimpleBlockFunction(Blocks.STRIPPED_OAK_LOG)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(EnhancedCommandsBlockTags.NETHER_STRIPPED_HYPHAE),
            new SimpleBlockFunction(Blocks.STRIPPED_OAK_WOOD)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(BlockTags.WART_BLOCKS),
            new SimpleBlockFunction(Blocks.OAK_LEAVES, List.of(new SimplePropertyFunction<>(LeavesBlock.PERSISTENT, false, false)))
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(EnhancedCommandsBlockTags.NETHER_VINES),
            new SimpleBlockFunction(Blocks.VINE)
        )));
    final ConditionalBlockFunction dirtOverlayCondition = new ConditionalBlockFunction(
        new AnyBlockPredicate(
            new HorizontalOffsetBlockPredicate(2, naturalizeIgnore),
            new HorizontalOffsetBlockPredicate(3, naturalizeIgnore),
            new HorizontalOffsetBlockPredicate(4, naturalizeIgnore)),
        new SimpleBlockFunction(Blocks.DIRT));
    context.add(of("overworld_plains"), new OverlayBlockFunction(new ReferenceBlockFunction(of("naturalize_vegetation")),
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(
                naturalizeIgnore,
                EmptyBlockFunction.INSTANCE
            ),
            new ConditionalBlockFunction(
                new HorizontalOffsetBlockPredicate(1, naturalizeIgnore),
                new SimpleBlockFunction(Blocks.GRASS_BLOCK)),
            dirtOverlayCondition,
            new ConditionalBlockFunction(
                new AnyBlockPredicate(new TagBlockPredicate(oresConventionalTag()), new SimpleBlockPredicate(Blocks.STONE)),
                EmptyBlockFunction.INSTANCE,
                new SimpleBlockFunction(Blocks.STONE)
            )
        )));
    context.add(of("overworld_mushroom"), new OverlayBlockFunction(new ReferenceBlockFunction(of("naturalize_vegetation")),
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(
                naturalizeIgnore,
                EmptyBlockFunction.INSTANCE
            ),
            new ConditionalBlockFunction(
                new HorizontalOffsetBlockPredicate(1, naturalizeIgnore),
                new SimpleBlockFunction(Blocks.MYCELIUM)),
            dirtOverlayCondition,
            new ConditionalBlockFunction(
                new AnyBlockPredicate(new TagBlockPredicate(oresConventionalTag()), new SimpleBlockPredicate(Blocks.STONE)),
                EmptyBlockFunction.INSTANCE,
                new SimpleBlockFunction(Blocks.STONE)
            )
        )));
    context.add(of("overworld_desert"),
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(
                naturalizeIgnore,
                EmptyBlockFunction.INSTANCE
            ),
            new ConditionalBlockFunction(
                new HorizontalOffsetBlockPredicate(2, naturalizeIgnore),
                new SimpleBlockFunction(Blocks.SAND)),
            new ConditionalBlockFunction(
                new AnyBlockPredicate(
                    new HorizontalOffsetBlockPredicate(3, naturalizeIgnore),
                    new HorizontalOffsetBlockPredicate(4, naturalizeIgnore),
                    new HorizontalOffsetBlockPredicate(5, naturalizeIgnore)),
                new SimpleBlockFunction(Blocks.SANDSTONE)),
            new ConditionalBlockFunction(
                new AnyBlockPredicate(new TagBlockPredicate(oresConventionalTag()), new SimpleBlockPredicate(Blocks.STONE)),
                EmptyBlockFunction.INSTANCE,
                new SimpleBlockFunction(Blocks.STONE)
            )
        ));
    context.add(of("air_checkerboard"), new CheckerboardBlockFunction(new WeightedList.Uniform<>(new SimpleBlockFunction(Blocks.AIR), EmptyBlockFunction.INSTANCE)));
    context.add(of("air_grid"), new CheckerboardBlockFunction(
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
    context.add(of("random_dried_block"), new DryBlockFunction(RandomBlockFunction.RANDOM_SEED));

    final ConditionalBlockFunction replaceDirtWithNetherrack = new ConditionalBlockFunction(
        new TagBlockPredicate(BlockTags.DIRT),
        new SimpleBlockFunction(Blocks.NETHERRACK)
    );
    final ConditionalBlockFunction replaceSandWithSoulSand = new ConditionalBlockFunction(
        new TagBlockPredicate(BlockTags.SAND),
        new SimpleBlockFunction(Blocks.SOUL_SAND)
    );
    context.add(of("crimsonize"), new PropertiesNbtCombinationBlockFunction(
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(new TagBlockPredicate(EnhancedCommandsBlockTags.OVERLAID_DIRT),
                new SimpleBlockFunction(Blocks.CRIMSON_NYLIUM)),
            replaceDirtWithNetherrack,
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
            replaceSandWithSoulSand
        ),
        new PropertyNamesBlockFunction(Collections.singletonList(new AllOriginalPropertyNameFunctions())),
        null
    ));
    context.add(of("warpize"), new PropertiesNbtCombinationBlockFunction(
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(new TagBlockPredicate(EnhancedCommandsBlockTags.OVERLAID_DIRT),
                new SimpleBlockFunction(Blocks.WARPED_NYLIUM)),
            replaceDirtWithNetherrack,
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
            replaceSandWithSoulSand
        ),
        new PropertyNamesBlockFunction(Collections.singletonList(new AllOriginalPropertyNameFunctions())),
        null
    ));
    context.add(of("concrete_to_powder"), new IdReplaceBlockFunction(Pattern.compile("_concrete$"), "_concrete_powder"));
    context.add(of("powder_to_concrete"), new IdReplaceBlockFunction(Pattern.compile("_concrete_powder$"), "_concrete"));


    context.add(of("red_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(EnhancedCommandsBlockTags.RED_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    context.add(of("orange_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(EnhancedCommandsBlockTags.ORANGE_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    context.add(of("yellow_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(EnhancedCommandsBlockTags.YELLOW_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    context.add(of("green_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(EnhancedCommandsBlockTags.GREEN_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    context.add(of("light_blue_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(EnhancedCommandsBlockTags.LIGHT_BLUE_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    context.add(of("blue_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(EnhancedCommandsBlockTags.BLUE_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    context.add(of("pink_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(EnhancedCommandsBlockTags.PINK_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    context.add(of("black_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(EnhancedCommandsBlockTags.BLACK_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    context.add(of("gray_colors"), new PropertiesNbtCombinationBlockFunction(new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new TagBlockFunction(wrapper.getOrThrow(EnhancedCommandsBlockTags.GRAY_COLORS)), 0.75), ObjectDoublePair.of(new TagBlockFunction(wrapper.getOrThrow(EnhancedCommandsBlockTags.DEAD_CORAL_BLOCK)), 0.25))), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    context.add(of("white_colors"), new PropertiesNbtCombinationBlockFunction(new TagBlockFunction(wrapper.getOrThrow(EnhancedCommandsBlockTags.WHITE_COLORS)), new PropertyNamesBlockFunction(new AllRandomPropertyNameFunction()), null));
    context.add(of("slightly_worn_stone_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.STONE_BRICKS), 0.8), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_STONE_BRICKS), 0.1), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.MOSSY_STONE_BRICKS), 0.1))));
    context.add(of("slightly_worn_nether_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.NETHER_BRICKS), 0.8), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_NETHER_BRICKS), 0.2))));
    context.add(of("slightly_worn_deepslate_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.DEEPSLATE_BRICKS), 0.8), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_DEEPSLATE_BRICKS), 0.2))));
    context.add(of("mediumly_worn_stone_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.STONE_BRICKS), 0.6), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_STONE_BRICKS), 0.2), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.MOSSY_STONE_BRICKS), 0.2))));
    context.add(of("mediumly_worn_nether_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.NETHER_BRICKS), 0.6), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_NETHER_BRICKS), 0.4))));
    context.add(of("mediumly_worn_deepslate_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.DEEPSLATE_BRICKS), 0.6), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_DEEPSLATE_BRICKS), 0.4))));
    context.add(of("heavily_worn_stone_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.STONE_BRICKS), 0.2), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_STONE_BRICKS), 0.4), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.MOSSY_STONE_BRICKS), 0.4))));
    context.add(of("heavily_worn_nether_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.NETHER_BRICKS), 0.2), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_NETHER_BRICKS), 0.8))));
    context.add(of("heavily_worn_deepslate_bricks"), new PickBlockFunction(new WeightedList.Weighted<>(ObjectDoublePair.of(new SimpleBlockFunction(Blocks.DEEPSLATE_BRICKS), 0.2), ObjectDoublePair.of(new SimpleBlockFunction(Blocks.CRACKED_DEEPSLATE_BRICKS), 0.8))));
  }

  @ExpectPlatform
  private static TagKey<Block> oresConventionalTag() {
    throw new AssertionError();
  }
}
