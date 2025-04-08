package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.math.Noise;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.RandomizedSeedHolder;

import java.util.Optional;
import java.util.OptionalLong;

public class NoiseBlockPredicate implements BlockPredicate, Noise {

  public static final MapCodec<NoiseBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      pers.solid.ecmd.math.WeightedList.createMapCodec(BlockPredicate.CODEC).fieldOf("list").forGetter(NoiseBlockPredicate::list),
      Codec.LONG.optionalFieldOf("seed").xmap(ol -> ol.map(OptionalLong::of).orElseGet(OptionalLong::empty), ol -> ol.isEmpty() ? Optional.empty() : Optional.of(ol.getAsLong())).forGetter(NoiseBlockPredicate::seed),
      DoublePerlinNoiseSampler.NoiseParameters.CODEC.fieldOf("parameters").forGetter(NoiseBlockPredicate::noiseParameters),
      Vec3d.CODEC.optionalFieldOf("scale", UNIT).forGetter(NoiseBlockPredicate::scale),
      Vec3d.CODEC.optionalFieldOf("offset", Vec3d.ZERO).forGetter(NoiseBlockPredicate::offset)
  ).apply(instance, NoiseBlockPredicate::new));
  private final RandomizedSeedHolder seedHolder;
  private final WeightedList<BlockPredicate> list;
  private final Properties properties;

  public NoiseBlockPredicate(WeightedList<BlockPredicate> list, Properties properties) {
    this.list = list;
    this.properties = properties;
    this.seedHolder = RandomizedSeedHolder.ofOptional(seed());
  }

  public NoiseBlockPredicate(WeightedList<BlockPredicate> list, OptionalLong seed, DoublePerlinNoiseSampler.NoiseParameters noiseParameters, Vec3d scale, Vec3d offset) {
    this(list, new Properties(seed, noiseParameters, scale, offset));
  }

  private NoiseBlockPredicate(WeightedList<BlockPredicate> list, RandomizedSeedHolder seedHolder, Properties properties) {
    this.list = list;
    this.properties = properties;
    this.seedHolder = seedHolder;
  }

  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof NoiseBlockPredicate that)) return false;

    return list.equals(that.list) && properties.equals(that.properties);
  }

  @Override
  public int hashCode() {
    int result = list.hashCode();
    result = 31 * result + properties.hashCode();
    return result;
  }

  public WeightedList<BlockPredicate> list() {
    return list;
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    final Random random = ((WorldAccess) cachedBlockPosition.getWorld()).getRandom();
    return sample(random, list, Vec3d.of(cachedBlockPosition.getBlockPos())).test(cachedBlockPosition);
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

  @Override
  public NoiseBlockPredicate getRefreshed(Random random) {
    return new NoiseBlockPredicate(list, seedHolder.getRefreshed(random), properties);
  }

  @Override
  public Properties properties() {
    return properties;
  }

  @Override
  public RandomizedSeedHolder seedHolder() {
    return seedHolder;
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
