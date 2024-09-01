package pers.solid.ecmd.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.function.block.*;
import pers.solid.ecmd.predicate.block.AnyBlockPredicate;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.HorizontalOffsetBlockPredicate;
import pers.solid.ecmd.predicate.block.ReferenceBlockPredicate;
import pers.solid.ecmd.util.WeightedList;

import java.util.Arrays;
import java.util.List;
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
    entries.add(of("black_white_checkerboard"), new CheckerboardBlockFunction(uniform(Blocks.WHITE_WOOL, Blocks.BLACK_WOOL)));
    entries.add(of("pride_checkerboard"), new CheckerboardBlockFunction(uniform(Blocks.RED_WOOL, Blocks.ORANGE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.BLUE_WOOL, Blocks.PURPLE_WOOL))); // 🏳️‍🌈
    entries.add(of("trans_checkerboard"), new CheckerboardBlockFunction(uniform(Blocks.LIGHT_BLUE_WOOL, Blocks.PINK_WOOL, Blocks.WHITE_WOOL, Blocks.PINK_WOOL))); // 🏳️‍⚧️
    final ReferenceBlockPredicate natualizePlaceable = new ReferenceBlockPredicate(RegistryEntry.Reference.standAlone(registries.getWrapperOrThrow(BlockPredicate.REGISTRY_KEY), RegistryKey.of(BlockPredicate.REGISTRY_KEY, EnhancedCommands.id("natualize_placeable"))));
    entries.add(of("natualize"), new ConditionalBlockFunction(natualizePlaceable, new ConditionalBlockFunction(new HorizontalOffsetBlockPredicate(1, natualizePlaceable), new SimpleBlockFunction(Blocks.GRASS_BLOCK), new ConditionalBlockFunction(new AnyBlockPredicate(List.of(new HorizontalOffsetBlockPredicate(2, natualizePlaceable), new HorizontalOffsetBlockPredicate(3, natualizePlaceable), new HorizontalOffsetBlockPredicate(4, natualizePlaceable))), new SimpleBlockFunction(Blocks.DIRT), new SimpleBlockFunction(Blocks.GLASS))), EmptyBlockFunction.INSTANCE));
    entries.add(of("air_checkerboard"), new CheckerboardBlockFunction(new WeightedList.Uniform<>(List.of(new SimpleBlockFunction(Blocks.AIR), EmptyBlockFunction.INSTANCE))));
    entries.add(of("air_grid"), new CheckerboardBlockFunction(new WeightedList.Uniform<>(List.of(
        new SimpleBlockFunction(Blocks.AIR),
        new CheckerboardBlockFunction(new WeightedList.Uniform<>(List.of(
            new SimpleBlockFunction(Blocks.AIR),
            new CheckerboardBlockFunction(new WeightedList.Uniform<>(List.of(
                new SimpleBlockFunction(Blocks.AIR),
                EmptyBlockFunction.INSTANCE
            )),
                Vec3d.ZERO,
                new Vec3d(1, 0, 0),
                Vec3d.ZERO
            ))),
            Vec3d.ZERO,
            new Vec3d(0, 1, 0),
            Vec3d.ZERO
        ))),
        Vec3d.ZERO,
        new Vec3d(0, 0, 1),
        Vec3d.ZERO
    ));
  }

  @Override
  public String getName() {
    return "Block Functions";
  }
}
