package pers.solid.ecmd.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.Vec3d;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.predicate.block.*;
import pers.solid.ecmd.util.WeightedList;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BlockPredicateDataGeneration extends FabricDynamicRegistryProvider {
  public BlockPredicateDataGeneration(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
    super(output, registriesFuture);
  }

  @Override
  protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
    entries.add(of("natualize_placeable"), new AnyBlockPredicate(List.of(
        new TagBlockPredicate(BlockTags.AIR),
        new SimpleBlockPredicate(Blocks.SHORT_GRASS),
        new TagBlockPredicate(BlockTags.FLOWERS)
    )));
    entries.add(of("air_checkerboard"), new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(ConstantBlockPredicate.ALWAYS_TRUE, BlockPredicate.EMPTY))));
    entries.add(of("air_grid"), new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(
        ConstantBlockPredicate.ALWAYS_TRUE,
        new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(
            ConstantBlockPredicate.ALWAYS_TRUE,
            new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(
                ConstantBlockPredicate.ALWAYS_TRUE,
                BlockPredicate.EMPTY
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

  protected static RegistryKey<BlockPredicate> of(String value) {
    return RegistryKey.of(BlockPredicate.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getName() {
    return "Block Predicate";
  }
}
