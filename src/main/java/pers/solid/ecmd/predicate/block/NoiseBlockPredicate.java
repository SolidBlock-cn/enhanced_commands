package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.Noise;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.*;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.List;
import java.util.OptionalLong;

public record NoiseBlockPredicate(WeightedList<BlockPredicate> list, Properties properties) implements BlockPredicate, Noise {

  public static final MapCodec<NoiseBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      WeightedList.createMapCodec(BlockPredicate.CODEC).fieldOf("list").forGetter(NoiseBlockPredicate::list),
      CodecUtil.optionalLongFieldOf("seed").forGetter(NoiseBlockPredicate::seed),
      NormalNoise.NoiseParameters.DIRECT_CODEC.fieldOf("parameters").forGetter(NoiseBlockPredicate::noiseParameters),
      Vec3.CODEC.optionalFieldOf("scale", UNIT).forGetter(NoiseBlockPredicate::scale),
      Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(NoiseBlockPredicate::offset)
  ).apply(instance, NoiseBlockPredicate::new));


  public NoiseBlockPredicate(WeightedList<BlockPredicate> list, OptionalLong seed, NormalNoise.NoiseParameters noiseParameters, Vec3 scale, Vec3 offset) {
    this(list, new Properties(seed, noiseParameters, scale, offset));
  }

  @Override
  public boolean test(BlockInWorld cachedBlockPosition, ExecutionContext context) {
    return sample(seed().orElseGet(() -> context.getSeed(this)), list, Vec3.atLowerCornerOf(cachedBlockPosition.getPos())).test(cachedBlockPosition, context);
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld cachedBlockPosition, ExecutionContext context) {
    final BlockPos blockPos = cachedBlockPosition.getPos();
    final long actualSeed = seed().orElseGet(() -> context.getSeed(this));
    final double sampleValue = getSampleValue(actualSeed, blockPos.getX(), blockPos.getY(), blockPos.getZ());
    final TestResult testResult = list.getClampedElement(sampleValue).testAndDescribe(cachedBlockPosition, context);
    return TestResult.of(testResult.successes(), Component.translatable("enhanced_commands.block_predicate.noise.result", TextUtil.wrapVector(blockPos).withStyle(Styles.TARGET), TextUtil.literal(actualSeed).withStyle(Styles.TARGET), TextUtil.literal(sampleValue).withStyle(Styles.RESULT)), List.of(testResult));
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.NOISE;
  }

  @Override
  public @NotNull String asString() {
    final String listString = list.asString(ExpressionConvertible::asString);
    final String parametersString = asParametersString();
    return "noise(" + listString + (parametersString.isEmpty() ? "" : "; " + parametersString) + ")";
  }

  public enum Type implements BlockPredicateType<NoiseBlockPredicate> {
    NOISE_TYPE;

    @Override
    public @NotNull MapCodec<NoiseBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  public static class Parser extends Noise.Parser<BlockPredicate> {
    @Override
    protected BlockPredicate parseElement(ParseContext<?> parseContext) throws CommandSyntaxException {
      return BlockPredicate.parse(parseContext);
    }

    @Override
    public NoiseBlockPredicate getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      super.getParseResult(parseContext);
      return new NoiseBlockPredicate(weightedList, seed, new NormalNoise.NoiseParameters(firstOctave, amplitudes), scale, offset);
    }
  }
}
