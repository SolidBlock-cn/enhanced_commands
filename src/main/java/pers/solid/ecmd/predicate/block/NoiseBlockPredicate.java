package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.Noise;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.List;
import java.util.OptionalLong;

public record NoiseBlockPredicate(WeightedList<BlockPredicate> list, Properties properties) implements BlockPredicate, Noise {

  public static final MapCodec<NoiseBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      WeightedList.createMapCodec(BlockPredicate.CODEC).fieldOf("list").forGetter(NoiseBlockPredicate::list),
      CodecUtil.optionalLongFieldOf("seed").forGetter(NoiseBlockPredicate::seed),
      DoublePerlinNoiseSampler.NoiseParameters.CODEC.fieldOf("parameters").forGetter(NoiseBlockPredicate::noiseParameters),
      Vec3d.CODEC.optionalFieldOf("scale", UNIT).forGetter(NoiseBlockPredicate::scale),
      Vec3d.CODEC.optionalFieldOf("offset", Vec3d.ZERO).forGetter(NoiseBlockPredicate::offset)
  ).apply(instance, NoiseBlockPredicate::new));


  public NoiseBlockPredicate(WeightedList<BlockPredicate> list, OptionalLong seed, DoublePerlinNoiseSampler.NoiseParameters noiseParameters, Vec3d scale, Vec3d offset) {
    this(list, new Properties(seed, noiseParameters, scale, offset));
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    return sample(seed().orElseGet(() -> context.getSeed(this)), list, Vec3d.of(cachedBlockPosition.getBlockPos())).test(cachedBlockPosition, context);
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final BlockPos blockPos = cachedBlockPosition.getBlockPos();
    final long actualSeed = seed().orElseGet(() -> context.getSeed(this));
    final double sampleValue = getSampleValue(actualSeed, blockPos.getX(), blockPos.getY(), blockPos.getZ());
    final TestResult testResult = list.getClampedElement(sampleValue).testAndDescribe(cachedBlockPosition, context);
    return TestResult.of(testResult.successes(), Text.translatable("enhanced_commands.block_predicate.noise.result", TextUtil.wrapVector(blockPos).styled(Styles.TARGET), TextUtil.literal(actualSeed).styled(Styles.TARGET), TextUtil.literal(sampleValue).styled(Styles.RESULT)), List.of(testResult));
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
      return new NoiseBlockPredicate(weightedList, seed, new DoublePerlinNoiseSampler.NoiseParameters(firstOctave, amplitudes), scale, offset);
    }
  }
}
