package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.math.Noise;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.Optional;
import java.util.OptionalLong;

public record NoiseBlockPredicate(WeightedList<BlockPredicate> list, Properties properties) implements BlockPredicate, Noise {

  public static final MapCodec<NoiseBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      WeightedList.createMapCodec(BlockPredicate.CODEC).fieldOf("list").forGetter(NoiseBlockPredicate::list),
      Codec.LONG.optionalFieldOf("seed").xmap(ol -> ol.map(OptionalLong::of).orElseGet(OptionalLong::empty), ol -> ol.isEmpty() ? Optional.empty() : Optional.of(ol.getAsLong())).forGetter(NoiseBlockPredicate::seed),
      DoublePerlinNoiseSampler.NoiseParameters.CODEC.fieldOf("parameters").forGetter(NoiseBlockPredicate::noiseParameters),
      Vec3d.CODEC.optionalFieldOf("scale", UNIT).forGetter(NoiseBlockPredicate::scale),
      Vec3d.CODEC.optionalFieldOf("offset", Vec3d.ZERO).forGetter(NoiseBlockPredicate::offset)
  ).apply(instance, NoiseBlockPredicate::new));


  public NoiseBlockPredicate(WeightedList<BlockPredicate> list, OptionalLong seed, DoublePerlinNoiseSampler.NoiseParameters noiseParameters, Vec3d scale, Vec3d offset) {
    this(list, new Properties(seed, noiseParameters, scale, offset));
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition, BlockPredicateContext context) {
    return sample(context.getSeed(this), list, Vec3d.of(cachedBlockPosition.getBlockPos())).test(cachedBlockPosition, context);
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

  public static class Parser extends Noise.Parser<BlockPredicateArgument> {

    @Override
    protected BlockPredicateArgument parseElement(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly, boolean allowSparse) throws CommandSyntaxException {
      return BlockPredicateArgument.parse(registryAccess, parser, suggestionsOnly);
    }

    @Override
    public BlockPredicateArgument getParseResult(CommandRegistryAccess registryAccess, SuggestedParser<?> parser) throws CommandSyntaxException {
      return source -> new NoiseBlockPredicate(weightedList.transform(blockFunctionArgument -> blockFunctionArgument.apply(source)), seed, new DoublePerlinNoiseSampler.NoiseParameters(firstOctave, amplitudes), scale, offset);
    }
  }
}
