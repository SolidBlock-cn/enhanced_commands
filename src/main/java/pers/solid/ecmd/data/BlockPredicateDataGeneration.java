package pers.solid.ecmd.data;

import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.math.Noise;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.predicate.block.*;

import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class BlockPredicateDataGeneration extends FabricDynamicRegistryProvider {
  public BlockPredicateDataGeneration(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
    super(output, registriesFuture);
  }

  @Override
  protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
    entries.add(of("natualize_placeable"), new AnyBlockPredicate(List.of(
        new TagBlockPredicate(BlockTags.REPLACEABLE),
        new TagBlockPredicate(BlockTags.LEAVES),
        new TagBlockPredicate(BlockTags.WART_BLOCKS),
        new TagBlockPredicate(BlockTags.LOGS),
        new TagBlockPredicate(ConventionalBlockTags.BUDS),
        new SimpleBlockPredicate(Blocks.GLOWSTONE)
    )));
    entries.add(of("checkerboard"), new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(ConstantBlockPredicate.ALWAYS_TRUE, ConstantBlockPredicate.ALWAYS_FALSE))));
    entries.add(of("typical_noise"), new NoiseBlockPredicate(new WeightedList.Uniform<>(ConstantBlockPredicate.ALWAYS_TRUE, ConstantBlockPredicate.ALWAYS_FALSE), OptionalLong.empty(), new DoublePerlinNoiseSampler.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), Noise.UNIT, Vec3d.ZERO));
    entries.add(of("noise_uniform"), new NoiseBlockPredicate(new WeightedList.Uniform<>(ConstantBlockPredicate.ALWAYS_TRUE, ConstantBlockPredicate.ALWAYS_FALSE), OptionalLong.empty(), new DoublePerlinNoiseSampler.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3d(0.2, 0.2, 0.2), Vec3d.ZERO));
    entries.add(of("noise_most"), new NoiseBlockPredicate(new WeightedList.Weighted<>(ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_TRUE, 5), ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_FALSE, 1)), OptionalLong.empty(), new DoublePerlinNoiseSampler.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3d(0.2, 0.2, 0.2), Vec3d.ZERO));
    entries.add(of("noise_few"), new NoiseBlockPredicate(new WeightedList.Weighted<>(ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_TRUE, 1), ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_FALSE, 5)), OptionalLong.empty(), new DoublePerlinNoiseSampler.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3d(0.2, 0.2, 0.2), Vec3d.ZERO));
    entries.add(of("grid"), new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(
        ConstantBlockPredicate.ALWAYS_TRUE,
        new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(
            ConstantBlockPredicate.ALWAYS_TRUE,
            new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(
                ConstantBlockPredicate.ALWAYS_TRUE,
                ConstantBlockPredicate.ALWAYS_FALSE
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
    entries.add(of("redstone_related"), new IdContainBlockPredicate(Pattern.compile("redstone", Pattern.LITERAL)));
  }

  protected static RegistryKey<BlockPredicate> of(String value) {
    return RegistryKey.of(BlockPredicate.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getName() {
    return "Block Predicate";
  }
}
