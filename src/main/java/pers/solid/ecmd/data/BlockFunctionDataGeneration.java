package pers.solid.ecmd.data;

import net.minecraft.core.Holder;
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
import pers.solid.ecmd.property.function.SimplePropertyFunction;
import pers.solid.ecmd.property.predicate.Comparator;
import pers.solid.ecmd.property.predicate.ComparisonPropertyPredicate;
import pers.solid.ecmd.tag.EnhancedCommandsBlockTags;
import pers.solid.ecmd.util.pack.LazyReference;
import pers.solid.ecmd.util.pack.RegistryHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.regex.Pattern;

import static pers.solid.ecmd.util.pack.RegistryHelper.emptyNamedSet;

public class BlockFunctionDataGeneration implements DynamicRegistryGenerationBridge<BlockFunction> {
  private static ResourceKey<BlockFunction> of(String value) {
    return ResourceKey.create(BlockFunction.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  private static WeightedList.Uniform<BlockFunction> uniformSimple(Block... blocks) {
    return new WeightedList.Uniform<>(Arrays.stream(blocks).<BlockFunction>map(SimpleBlockFunction::new).toList());
  }

  @Override
  public String getBridgeName() {
    return "Block Functions (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<BlockFunction> context) {
    final WeightedList.Uniform<BlockFunction> blackWhiteConcretes = uniformSimple(Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE);
    final WeightedList.Uniform<BlockFunction> rainbowConcretes = uniformSimple(Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.MAGENTA_CONCRETE);

    context.add(of("checkerboard/black_white"), new CheckerboardBlockFunction(blackWhiteConcretes));
    context.add(of("checkerboard/black_white_large"), new CheckerboardBlockFunction(blackWhiteConcretes, new Vec3(4, 4, 4), Checkerboard.UNIT, Checkerboard.UNIT));
    context.add(of("checkerboard/black_white_strip"), new CheckerboardBlockFunction(blackWhiteConcretes, Checkerboard.UNIT, new Vec3(0.25f, 0.25f, 0.25f), Vec3.ZERO));
    context.add(of("checkerboard/rainbow"), new CheckerboardBlockFunction(rainbowConcretes));
    context.add(of("checkerboard/rainbow_large"), new CheckerboardBlockFunction(rainbowConcretes, Checkerboard.UNIT, new Vec3(1 / 3f, 1 / 3f, 1 / 3f), Vec3.ZERO));
    context.add(of("noise/black_white"), new NoiseBlockFunction(blackWhiteConcretes, OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), Noise.UNIT, Vec3.ZERO));
    context.add(of("noise/black_white_large"), new NoiseBlockFunction(blackWhiteConcretes, OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.25f, 0.25f, 0.25f), Vec3.ZERO));
    context.add(of("noise/rainbow"), new NoiseBlockFunction(rainbowConcretes, OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.25f, 0.25f, 0.25f), Vec3.ZERO));
    context.add(of("noise/rainbow_largs"), new NoiseBlockFunction(rainbowConcretes, OptionalLong.empty(), new NormalNoise.NoiseParameters(-2, Noise.DEFAULT_AMPLITUDES), new Vec3(0.125f, 0.125f, 0.125f), Vec3.ZERO));

    final var naturalizeIgnored = new TagBlockPredicate(emptyNamedSet(EnhancedCommandsBlockTags.NATURALIZE_IGNORED));
    final HolderLookup.RegistryLookup<Block> wrapper = context.registryLookup(Registries.BLOCK).orElseThrow();

    context.add(of("naturalize/vegetation_conversion"), new ConditionsBlockFunction(
        new ConditionalBlockFunction(
            new AnyBlockPredicate(new TagBlockPredicate(emptyNamedSet(EnhancedCommandsBlockTags.NETHER_FUNGUS)), new SimpleBlockPredicate(Blocks.DEAD_BUSH)),
            new TagBlockFunction(wrapper.getOrThrow(BlockTags.SMALL_FLOWERS))
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(emptyNamedSet(EnhancedCommandsBlockTags.NETHER_ROOTS)),
            new SimpleBlockFunction(Blocks.SHORT_GRASS)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(emptyNamedSet(EnhancedCommandsBlockTags.NETHER_NATURAL_STEM)),
            new SimpleBlockFunction(Blocks.OAK_LOG)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(emptyNamedSet(EnhancedCommandsBlockTags.NETHER_NATURAL_HYPHAE)),
            new SimpleBlockFunction(Blocks.OAK_WOOD)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(emptyNamedSet(EnhancedCommandsBlockTags.NETHER_STRIPPED_STEM)),
            new SimpleBlockFunction(Blocks.STRIPPED_OAK_LOG)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(emptyNamedSet(EnhancedCommandsBlockTags.NETHER_STRIPPED_HYPHAE)),
            new SimpleBlockFunction(Blocks.STRIPPED_OAK_WOOD)
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(emptyNamedSet(BlockTags.WART_BLOCKS)),
            new SimpleBlockFunction(Blocks.OAK_LEAVES, List.of(new SimplePropertyFunction<>(LeavesBlock.PERSISTENT, false, false)))
        ),
        new ConditionalBlockFunction(
            new TagBlockPredicate(emptyNamedSet(EnhancedCommandsBlockTags.NETHER_VINES)),
            new SimpleBlockFunction(Blocks.VINE)
        )));

    final ConditionalBlockFunction dirtOverlayCondition = new ConditionalBlockFunction(
        new AnyBlockPredicate(
            new HorizontalOffsetBlockPredicate(2, naturalizeIgnored),
            new HorizontalOffsetBlockPredicate(3, naturalizeIgnored),
            new HorizontalOffsetBlockPredicate(4, naturalizeIgnored)),
        new SimpleBlockFunction(Blocks.DIRT));

    final Holder.Reference<BlockFunction> vegetationConversion = LazyReference.of(of("naturalize/vegetation_conversion"));

    final AnyBlockPredicate lowerGrasses = new AnyBlockPredicate(new SimpleBlockPredicate(Blocks.SHORT_GRASS), new SimpleBlockPredicate(Blocks.FERN), new SimpleBlockPredicate(Blocks.TALL_GRASS, List.of(new ComparisonPropertyPredicate<>(BlockStateProperties.DOUBLE_BLOCK_HALF, Comparator.EQ, DoubleBlockHalf.LOWER))));
    context.add(of("naturalize/plains"), new OverlayBlockFunction(new ReferenceBlockFunction(vegetationConversion),
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(
                naturalizeIgnored,
                EmptyBlockFunction.INSTANCE
            ),
            new ConditionalBlockFunction(
                new HorizontalOffsetBlockPredicate(1, naturalizeIgnored),
                new SimpleBlockFunction(Blocks.GRASS_BLOCK)),
            dirtOverlayCondition,
            new ConditionalBlockFunction(
                new AnyBlockPredicate(new TagBlockPredicate(emptyNamedSet(SharedCommonTags.ores())), new SimpleBlockPredicate(Blocks.STONE)),
                EmptyBlockFunction.INSTANCE,
                new SimpleBlockFunction(Blocks.STONE)
            ),
            new ConditionalBlockFunction(
                new AnyBlockPredicate(
                    new SimpleBlockPredicate(Blocks.BROWN_MUSHROOM),
                    new SimpleBlockPredicate(Blocks.DEAD_BUSH),
                    new TagBlockPredicate(emptyNamedSet(EnhancedCommandsBlockTags.NETHER_ROOTS))
                ),
                new SimpleBlockFunction(Blocks.SHORT_GRASS)
            ),
            new ConditionalBlockFunction(
                new AnyBlockPredicate(
                    new SimpleBlockPredicate(Blocks.RED_MUSHROOM),
                    new TagBlockPredicate(emptyNamedSet(EnhancedCommandsBlockTags.NETHER_FUNGUS))
                ),
                new TagBlockFunction(emptyNamedSet(BlockTags.SMALL_FLOWERS))
            )
        )));

    context.add(of("naturalize/mushroom"), new OverlayBlockFunction(new ReferenceBlockFunction(vegetationConversion),
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(
                naturalizeIgnored,
                EmptyBlockFunction.INSTANCE
            ),
            new ConditionalBlockFunction(
                lowerGrasses,
                new SimpleBlockFunction(Blocks.BROWN_MUSHROOM)
            ),
            new ConditionalBlockFunction(
                new TagBlockPredicate(RegistryHelper.emptyNamedSet(BlockTags.SMALL_FLOWERS)),
                new SimpleBlockFunction(Blocks.RED_MUSHROOM)
            ),
            new ConditionalBlockFunction(
                new HorizontalOffsetBlockPredicate(1, naturalizeIgnored),
                new SimpleBlockFunction(Blocks.MYCELIUM)),
            dirtOverlayCondition,
            new ConditionalBlockFunction(
                new AnyBlockPredicate(new TagBlockPredicate(emptyNamedSet(SharedCommonTags.ores())), new SimpleBlockPredicate(Blocks.STONE)),
                EmptyBlockFunction.INSTANCE,
                new SimpleBlockFunction(Blocks.STONE)
            )
        )));

    context.add(of("naturalize/desert"),
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(
                naturalizeIgnored,
                EmptyBlockFunction.INSTANCE
            ),
            new ConditionalBlockFunction(
                new HorizontalOffsetBlockPredicate(2, naturalizeIgnored),
                new SimpleBlockFunction(Blocks.SAND)),
            new ConditionalBlockFunction(
                new AnyBlockPredicate(
                    new HorizontalOffsetBlockPredicate(3, naturalizeIgnored),
                    new HorizontalOffsetBlockPredicate(4, naturalizeIgnored),
                    new HorizontalOffsetBlockPredicate(5, naturalizeIgnored)),
                new SimpleBlockFunction(Blocks.SANDSTONE)),
            new ConditionalBlockFunction(
                new AnyBlockPredicate(new TagBlockPredicate(emptyNamedSet(SharedCommonTags.ores())), new SimpleBlockPredicate(Blocks.STONE)),
                EmptyBlockFunction.INSTANCE,
                new SimpleBlockFunction(Blocks.STONE)
            )
        ));

    final ConditionalBlockFunction replaceDirtWithNetherrack = new ConditionalBlockFunction(
        new TagBlockPredicate(emptyNamedSet(BlockTags.DIRT)),
        new SimpleBlockFunction(Blocks.NETHERRACK)
    );
    final ConditionalBlockFunction replaceSandWithSoulSand = new ConditionalBlockFunction(
        new TagBlockPredicate(emptyNamedSet(BlockTags.SAND)),
        new SimpleBlockFunction(Blocks.SOUL_SAND)
    );

    context.add(of("naturalize/crimson"), new PropertiesNbtCombinationBlockFunction(
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(new TagBlockPredicate(emptyNamedSet(EnhancedCommandsBlockTags.OVERLAID_DIRT)),
                new SimpleBlockFunction(Blocks.CRIMSON_NYLIUM)),
            replaceDirtWithNetherrack,
            new ConditionalBlockFunction(
                new TagBlockPredicate(emptyNamedSet(BlockTags.FLOWERS)),
                new SimpleBlockFunction(Blocks.CRIMSON_FUNGUS)
            ),
            new ConditionalBlockFunction(
                lowerGrasses,
                new SimpleBlockFunction(Blocks.CRIMSON_ROOTS)
            ),
            new ConditionalBlockFunction(
                new TagBlockPredicate(emptyNamedSet(BlockTags.OVERWORLD_NATURAL_LOGS)),
                new SimpleBlockFunction(Blocks.CRIMSON_STEM)
            ),
            new ConditionalBlockFunction(
                new TagBlockPredicate(emptyNamedSet(BlockTags.LEAVES)),
                new SimpleBlockFunction(Blocks.NETHER_WART_BLOCK),
                EmptyBlockFunction.INSTANCE
            ),
            replaceSandWithSoulSand
        ),
        new PropertyNamesBlockFunction(Collections.singletonList(new AllOriginalPropertyNameFunctions())),
        null
    ));

    context.add(of("naturalize/warped"), new PropertiesNbtCombinationBlockFunction(
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(new TagBlockPredicate(emptyNamedSet(EnhancedCommandsBlockTags.OVERLAID_DIRT)),
                new SimpleBlockFunction(Blocks.WARPED_NYLIUM)),
            replaceDirtWithNetherrack,
            new ConditionalBlockFunction(
                new TagBlockPredicate(emptyNamedSet(BlockTags.FLOWERS)),
                new SimpleBlockFunction(Blocks.WARPED_FUNGUS)
            ),
            new ConditionalBlockFunction(
                lowerGrasses,
                new SimpleBlockFunction(Blocks.WARPED_ROOTS)
            ),
            new ConditionalBlockFunction(
                new TagBlockPredicate(emptyNamedSet(BlockTags.OVERWORLD_NATURAL_LOGS)),
                new SimpleBlockFunction(Blocks.WARPED_STEM)
            ),
            new ConditionalBlockFunction(
                new TagBlockPredicate(emptyNamedSet(BlockTags.LEAVES)),
                new SimpleBlockFunction(Blocks.WARPED_WART_BLOCK),
                EmptyBlockFunction.INSTANCE
            ),
            replaceSandWithSoulSand
        ),
        new PropertyNamesBlockFunction(Collections.singletonList(new AllOriginalPropertyNameFunctions())),
        null
    ));

    context.add(of("conversion/concrete_to_powder"), new IdReplaceBlockFunction(Pattern.compile("_concrete$"), "_concrete_powder"));
    context.add(of("conversion/powder_to_concrete"), new IdReplaceBlockFunction(Pattern.compile("_concrete_powder$"), "_concrete"));


    context.add(of("checkerboard/air"), new CheckerboardBlockFunction(new WeightedList.Uniform<>(new SimpleBlockFunction(Blocks.AIR), EmptyBlockFunction.INSTANCE)));
    context.add(of("checkerboard/air_grid"), new CheckerboardBlockFunction(
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
  }
}
