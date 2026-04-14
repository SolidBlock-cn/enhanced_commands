package pers.solid.ecmd.block.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.phys.Vec3;
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
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    return sample(seed().orElseGet(() -> executionContext.getSeed(this)), list, Vec3.atLowerCornerOf(blockInWorld.getPos())).test(blockInWorld, executionContext);
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final BlockPos blockPos = blockInWorld.getPos();
    final long actualSeed = seed().orElseGet(() -> executionContext.getSeed(this));
    final double sampleValue = getSampleValue(actualSeed, blockPos.getX(), blockPos.getY(), blockPos.getZ());
    final TestResult testResult = list.getClampedElement(sampleValue).testAndDescribe(blockInWorld, executionContext);
    return TestResult.of(testResult.successes(), Component.translatable("enhanced_commands.block_predicate.noise.result", TextUtil.wrapVector(blockPos).withStyle(Styles.TARGET), TextUtil.literal(actualSeed).withStyle(Styles.TARGET), TextUtil.literal(sampleValue).withStyle(Styles.RESULT)), List.of(testResult));
  }

  @Override
  public BlockPredicateType<?> getType() {
    return BlockPredicateTypes.NOISE;
  }

  @Override
  public String asString() {
    final String listString = list.asString(ExpressionConvertible::asString);
    final String parametersString = asParametersString();
    return "noise(" + listString + (parametersString.isEmpty() ? "" : "; " + parametersString) + ")";
  }

  public enum Type implements BlockPredicateType<NoiseBlockPredicate> {
    NOISE_TYPE;

    @Override
    public MapCodec<NoiseBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  public static class Parser extends Noise.Parser<BlockPredicate> {
    @Override
    protected BlockPredicate parseElement(ParseContext<?> parseContext) throws CommandSyntaxException {
      return BlockPredicate.parse(parseContext);
    }

    @Override
    public BlockPredicate getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      super.getParseResult(parseContext);
      return new NoiseBlockPredicate(weightedList, seed, new NormalNoise.NoiseParameters(firstOctave, amplitudes), scale, offset);
    }
  }
}
