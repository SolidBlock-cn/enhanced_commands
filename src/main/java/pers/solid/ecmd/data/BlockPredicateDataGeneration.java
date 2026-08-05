package pers.solid.ecmd.data;

import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.block.predicate.*;
import pers.solid.ecmd.math.Noise;
import pers.solid.ecmd.math.WeightedList;

import java.util.List;
import java.util.OptionalLong;
import java.util.regex.Pattern;

public class BlockPredicateDataGeneration implements DynamicRegistryGenerationBridge<BlockPredicate> {

  private static ResourceKey<BlockPredicate> of(String value) {
    return ResourceKey.create(BlockPredicate.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "Block Predicates (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<BlockPredicate> context) {
    context.add(of("checkerboard"), new CheckerboardBlockPredicate(new WeightedList.Uniform<>(List.of(ConstantBlockPredicate.ALWAYS_TRUE, ConstantBlockPredicate.ALWAYS_FALSE))));
    context.add(of("noise/default"), new NoiseBlockPredicate(new WeightedList.Uniform<>(ConstantBlockPredicate.ALWAYS_TRUE, ConstantBlockPredicate.ALWAYS_FALSE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), Noise.UNIT, Vec3.ZERO));
    context.add(of("noise/uniform"), new NoiseBlockPredicate(new WeightedList.Uniform<>(ConstantBlockPredicate.ALWAYS_TRUE, ConstantBlockPredicate.ALWAYS_FALSE), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.2, 0.2, 0.2), Vec3.ZERO));
    context.add(of("noise/most"), new NoiseBlockPredicate(new WeightedList.Weighted<>(ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_TRUE, 5), ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_FALSE, 1)), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.2, 0.2, 0.2), Vec3.ZERO));
    context.add(of("noise/few"), new NoiseBlockPredicate(new WeightedList.Weighted<>(ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_TRUE, 1), ObjectDoublePair.of(ConstantBlockPredicate.ALWAYS_FALSE, 5)), OptionalLong.empty(), new NormalNoise.NoiseParameters(-1, Noise.DEFAULT_AMPLITUDES), new Vec3(0.2, 0.2, 0.2), Vec3.ZERO));
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
}
