package pers.solid.ecmd.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.Vec3d;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.function.block.*;
import pers.solid.ecmd.function.property.AllOriginalPropertyNameFunctions;
import pers.solid.ecmd.predicate.block.*;
import pers.solid.ecmd.util.WeightedList;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

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
    entries.add(of("black_white_checkerboard"), new CheckerboardBlockFunction(uniform(Blocks.WHITE_WOOL, Blocks.BLACK_WOOL), Vec3d.ZERO, new Vec3d(2, 2, 2), Vec3d.ZERO));
    entries.add(of("pride_checkerboard"), new CheckerboardBlockFunction(uniform(Blocks.RED_WOOL, Blocks.ORANGE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.BLUE_WOOL, Blocks.PURPLE_WOOL), Vec3d.ZERO, new Vec3d(2, 2, 2), Vec3d.ZERO)); // 🏳️‍🌈
    entries.add(of("trans_checkerboard"), new CheckerboardBlockFunction(uniform(Blocks.LIGHT_BLUE_WOOL, Blocks.PINK_WOOL, Blocks.WHITE_WOOL, Blocks.PINK_WOOL), Vec3d.ZERO, new Vec3d(2, 2, 2), Vec3d.ZERO)); // 🏳️‍⚧️
    final var natualizeConvertible = new AnyBlockPredicate(
        new SimpleBlockPredicate(Blocks.WARPED_FUNGUS),
        new SimpleBlockPredicate(Blocks.WARPED_ROOTS),
        new SimpleBlockPredicate(Blocks.CRIMSON_FUNGUS),
        new SimpleBlockPredicate(Blocks.CRIMSON_ROOTS)
    );
    final var natualizePlaceable = new AnyBlockPredicate(
        new ReferenceBlockPredicate(RegistryKey.of(BlockPredicate.REGISTRY_KEY, EnhancedCommands.id("natualize_placeable"))),
        natualizeConvertible
    );
    entries.add(of("natualize"), new ConditionsBlockFunction(
        new ConditionalBlockFunction(
            new AnyBlockPredicate(new TagBlockPredicate(BlockTags.AIR), new SimpleBlockPredicate(Blocks.WATER), new SimpleBlockPredicate(Blocks.LAVA)),
            EmptyBlockFunction.INSTANCE
        ),
        new ConditionalBlockFunction(
            new AnyBlockPredicate(new SimpleBlockPredicate(Blocks.CRIMSON_FUNGUS), new SimpleBlockPredicate(Blocks.WARPED_FUNGUS)),
            new TagBlockFunction(registries.getWrapperOrThrow(RegistryKeys.BLOCK).getOrThrow(BlockTags.FLOWERS))
        ),
        new ConditionalBlockFunction(
            new AnyBlockPredicate(new SimpleBlockPredicate(Blocks.CRIMSON_ROOTS), new SimpleBlockPredicate(Blocks.WARPED_ROOTS)),
            new SimpleBlockFunction(Blocks.SHORT_GRASS)
        ),
        new ConditionalBlockFunction(
            new HorizontalOffsetBlockPredicate(1, natualizePlaceable),
            new SimpleBlockFunction(Blocks.GRASS_BLOCK)),
        new ConditionalBlockFunction(
            new AnyBlockPredicate(
                new HorizontalOffsetBlockPredicate(2, natualizePlaceable),
                new HorizontalOffsetBlockPredicate(3, natualizePlaceable),
                new HorizontalOffsetBlockPredicate(4, natualizePlaceable)),
            new SimpleBlockFunction(Blocks.DIRT),
            new SimpleBlockFunction(Blocks.GRASS_BLOCK))));
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
                new Vec3d(0, 1, 1),
                Vec3d.ZERO
                )),
            Vec3d.ZERO,
            new Vec3d(1, 0, 1),
            Vec3d.ZERO
            )),
        Vec3d.ZERO,
        new Vec3d(1, 0, 0),
        Vec3d.ZERO
    ));
    entries.add(of("any_dried_block"), new DryBlockFunction(new RandomBlockFunction()));
    entries.add(of("white_gray_stone_checker"), new CheckerboardBlockFunction(new WeightedList.Uniform<>(
        new PickBlockFunction(uniform(Blocks.CALCITE, Blocks.DIORITE)),
        new PickBlockFunction(uniform(Blocks.STONE, Blocks.ANDESITE))
    ), Vec3d.ZERO, new Vec3d(3, 3, 3), Vec3d.ZERO));
    entries.add(of("crimsonize"), new PropertiesNbtCombinationBlockFunction(
        new ConditionsBlockFunction(
            new ConditionalBlockFunction(new AnyBlockPredicate(new SimpleBlockPredicate(Blocks.GRASS_BLOCK), new SimpleBlockPredicate(Blocks.MYCELIUM)),
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
                new AnyBlockPredicate(new SimpleBlockPredicate(Blocks.SHORT_GRASS), new SimpleBlockPredicate(Blocks.FERN)),
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
            new ConditionalBlockFunction(new AnyBlockPredicate(new SimpleBlockPredicate(Blocks.GRASS_BLOCK), new SimpleBlockPredicate(Blocks.MYCELIUM)),
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
                new AnyBlockPredicate(new SimpleBlockPredicate(Blocks.SHORT_GRASS), new SimpleBlockPredicate(Blocks.FERN)),
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
  }

  @Override
  public String getName() {
    return "Block Functions";
  }
}
