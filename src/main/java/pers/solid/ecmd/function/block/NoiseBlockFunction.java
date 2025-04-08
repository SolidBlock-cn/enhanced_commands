package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.math.Noise;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.RandomizedSeedHolder;

import java.util.Optional;
import java.util.OptionalLong;

public class NoiseBlockFunction implements BlockFunction, Noise {
  public static final MapCodec<NoiseBlockFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      WeightedList.createMapCodec(BlockFunction.CODEC).fieldOf("list").forGetter(NoiseBlockFunction::list),
      Codec.LONG.optionalFieldOf("seed").xmap(ol -> ol.map(OptionalLong::of).orElseGet(OptionalLong::empty), ol -> ol.isEmpty() ? Optional.empty() : Optional.of(ol.getAsLong())).forGetter(NoiseBlockFunction::seed),
      DoublePerlinNoiseSampler.NoiseParameters.CODEC.fieldOf("parameters").forGetter(NoiseBlockFunction::noiseParameters),
      Vec3d.CODEC.optionalFieldOf("scale", UNIT).forGetter(NoiseBlockFunction::scale),
      Vec3d.CODEC.optionalFieldOf("offset", Vec3d.ZERO).forGetter(NoiseBlockFunction::offset)
  ).apply(instance, NoiseBlockFunction::new));
  private final RandomizedSeedHolder seedHolder;
  private final WeightedList<BlockFunction> list;
  private final Properties properties;

  public NoiseBlockFunction(WeightedList<BlockFunction> list, OptionalLong seed, DoublePerlinNoiseSampler.NoiseParameters noiseParameters, Vec3d scale, Vec3d offset) {
    this(list, new Properties(seed, noiseParameters, scale, offset));
  }

  public NoiseBlockFunction(WeightedList<BlockFunction> list, Properties properties) {
    this.list = list;
    this.properties = properties;
    this.seedHolder = RandomizedSeedHolder.ofOptional(seed());
  }

  private NoiseBlockFunction(WeightedList<BlockFunction> list, RandomizedSeedHolder seedHolder, Properties properties) {
    this.list = list;
    this.properties = properties;
    this.seedHolder = seedHolder;
  }

  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof NoiseBlockFunction that)) return false;

    return list.equals(that.list) && properties.equals(that.properties);
  }

  @Override
  public int hashCode() {
    int result = list.hashCode();
    result = 31 * result + properties.hashCode();
    return result;
  }

  public WeightedList<BlockFunction> list() {
    return list;
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    return sample(world.getRandom(), list, Vec3d.of(pos)).getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
  }

  @Override
  public @NotNull BlockFunctionType<NoiseBlockFunction> getType() {
    return BlockFunctionTypes.NOISE;
  }

  @Override
  public @NotNull String asString() {
    final String listString = list.asString(ExpressionConvertible::asString);
    final String parametersString = asParametersString();
    return "noise(" + listString + (parametersString.isEmpty() ? "" : "; " + parametersString) + ")";
  }

  @Override
  public NoiseBlockFunction getRefreshed(Random random) {
    return new NoiseBlockFunction(list, seedHolder.getRefreshed(random), properties);
  }

  @Override
  public Properties properties() {
    return properties;
  }

  @Override
  public RandomizedSeedHolder seedHolder() {
    return seedHolder;
  }

  public enum Type implements BlockFunctionType<NoiseBlockFunction> {
    INSTANCE;

    @Override
    public @NotNull MapCodec<NoiseBlockFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser extends Noise.Parser<BlockFunctionArgument> {

    @Override
    protected BlockFunctionArgument parseElement(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly, boolean allowSparse) throws CommandSyntaxException {
      return BlockFunctionArgument.parse(registryAccess, parser, suggestionsOnly, true);
    }

    @Override
    public BlockFunctionArgument getParseResult(CommandRegistryAccess registryAccess, SuggestedParser<?> parser) throws CommandSyntaxException {
      return source -> new NoiseBlockFunction(weightedList.transform(blockFunctionArgument -> blockFunctionArgument.apply(source)), seed, new DoublePerlinNoiseSampler.NoiseParameters(firstOctave, amplitudes), scale, offset);
    }
  }
}
