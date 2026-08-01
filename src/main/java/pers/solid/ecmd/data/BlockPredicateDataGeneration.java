package pers.solid.ecmd.data;

import dev.architectury.injectables.annotations.ExpectPlatform;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.block.predicate.*;
import pers.solid.ecmd.math.Noise;
import pers.solid.ecmd.math.WeightedList;

import java.util.List;
import java.util.OptionalLong;
import java.util.regex.Pattern;

import static pers.solid.ecmd.data.DynamicRegistryGenerationBridge.emptyNamedSet;

public interface BlockPredicateDataGeneration extends DynamicRegistryGenerationBridge<BlockPredicate> {
  @ExpectPlatform
  static TagKey<Block> conventionalBudsTag() {
    throw new AssertionError();
  }

  @Override
  default void configureBridge(ContextBridge<BlockPredicate> context) {
    context.add(of("natualize_placeable"), new AnyBlockPredicate(List.of(
        new TagBlockPredicate(emptyNamedSet(BlockTags.REPLACEABLE)),
        new TagBlockPredicate(emptyNamedSet(BlockTags.LEAVES)),
        new TagBlockPredicate(emptyNamedSet(BlockTags.WART_BLOCKS)),
        new TagBlockPredicate(emptyNamedSet(BlockTags.LOGS)),
        new TagBlockPredicate(emptyNamedSet(conventionalBudsTag())),
        new SimpleBlockPredicate(Blocks.GLOWSTONE)
    )));
    context.add(of("checkerboard"), new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(ConstantBlockPredicate.ALWAYS_TRUE, ConstantBlockPredicate.ALWAYS_FALSE))));
    context.add(of("black_white_noise"), new NoiseBlockPredicate(new WeightedList.Uniform<>(ConstantBlockPredicate.ALWAYS_TRUE, ConstantBlockPredicate.ALWAYS_FALSE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), Noise.UNIT, Vec3.ZERO));
    context.add(of("noise_uniform"), new NoiseBlockPredicate(new WeightedList.Uniform<>(ConstantBlockPredicate.ALWAYS_TRUE, ConstantBlockPredicate.ALWAYS_FALSE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.2, 0.2, 0.2), Vec3.ZERO));
    context.add(of("noise_most"), new NoiseBlockPredicate(new WeightedList.Weighted<>(ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_TRUE, 5), ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_FALSE, 1)), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.2, 0.2, 0.2), Vec3.ZERO));
    context.add(of("noise_few"), new NoiseBlockPredicate(new WeightedList.Weighted<>(ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_TRUE, 1), ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_FALSE, 5)), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.2, 0.2, 0.2), Vec3.ZERO));
    context.add(of("grid"), new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(
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
    context.add(of("redstone_related"), new IdContainBlockPredicate(Pattern.compile("redstone", Pattern.LITERAL)));
  }

  static ResourceKey<BlockPredicate> of(String value) {
    return ResourceKey.create(BlockPredicate.REGISTRY_KEY, EnhancedCommands.id(value));
  }
}
