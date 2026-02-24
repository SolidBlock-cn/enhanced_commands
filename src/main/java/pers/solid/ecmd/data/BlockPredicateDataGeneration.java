package pers.solid.ecmd.data;

import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.math.Noise;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.predicate.block.*;

import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class BlockPredicateDataGeneration extends FabricDynamicRegistryProvider {
  public BlockPredicateDataGeneration(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, registriesFuture);
  }

  @Override
  protected void configure(HolderLookup.Provider registries, Entries entries) {
    entries.add(of("natualize_placeable"), new AnyBlockPredicate(List.of(
        new TagBlockPredicate(BlockTags.REPLACEABLE),
        new TagBlockPredicate(BlockTags.LEAVES),
        new TagBlockPredicate(BlockTags.WART_BLOCKS),
        new TagBlockPredicate(BlockTags.LOGS),
        new TagBlockPredicate(ConventionalBlockTags.BUDS),
        new SimpleBlockPredicate(Blocks.GLOWSTONE)
    )));
    entries.add(of("checkerboard"), new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(ConstantBlockPredicate.ALWAYS_TRUE, ConstantBlockPredicate.ALWAYS_FALSE))));
    entries.add(of("typical_noise"), new NoiseBlockPredicate(new WeightedList.Uniform<>(ConstantBlockPredicate.ALWAYS_TRUE, ConstantBlockPredicate.ALWAYS_FALSE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), Noise.UNIT, Vec3.ZERO));
    entries.add(of("noise_uniform"), new NoiseBlockPredicate(new WeightedList.Uniform<>(ConstantBlockPredicate.ALWAYS_TRUE, ConstantBlockPredicate.ALWAYS_FALSE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.2, 0.2, 0.2), Vec3.ZERO));
    entries.add(of("noise_most"), new NoiseBlockPredicate(new WeightedList.Weighted<>(ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_TRUE, 5), ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_FALSE, 1)), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.2, 0.2, 0.2), Vec3.ZERO));
    entries.add(of("noise_few"), new NoiseBlockPredicate(new WeightedList.Weighted<>(ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_TRUE, 1), ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_FALSE, 5)), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.2, 0.2, 0.2), Vec3.ZERO));
    entries.add(of("grid"), new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(
        ConstantBlockPredicate.ALWAYS_TRUE,
        new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(
            ConstantBlockPredicate.ALWAYS_TRUE,
            new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(
                ConstantBlockPredicate.ALWAYS_TRUE,
                ConstantBlockPredicate.ALWAYS_FALSE
            )),
                Vec3.ZERO,
                new Vec3(1, 0, 0),
                Vec3.ZERO
            ))),
            Vec3.ZERO,
            new Vec3(0, 1, 0),
            Vec3.ZERO
        ))),
        Vec3.ZERO,
        new Vec3(0, 0, 1),
        Vec3.ZERO
    ));
    entries.add(of("redstone_related"), new IdContainBlockPredicate(Pattern.compile("redstone", Pattern.LITERAL)));
  }

  protected static ResourceKey<BlockPredicate> of(String value) {
    return ResourceKey.create(BlockPredicate.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public @NotNull String getName() {
    return "Block Predicate";
  }
}
