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
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.math.Noise;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.Optional;
import java.util.OptionalLong;

public record NoiseBlockFunction(OptionalLong seed, DoublePerlinNoiseSampler.NoiseParameters noiseParameters, Vec3d scale, WeightedList<BlockFunction> list) implements BlockFunction, Noise {
  public static final MapCodec<NoiseBlockFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Codec.LONG.optionalFieldOf("seed").xmap(ol -> ol.map(OptionalLong::of).orElseGet(OptionalLong::empty), ol -> ol.isEmpty() ? Optional.empty() : Optional.of(ol.getAsLong())).forGetter(NoiseBlockFunction::seed),
      DoublePerlinNoiseSampler.NoiseParameters.CODEC.fieldOf("parameters").forGetter(NoiseBlockFunction::noiseParameters),
      Vec3d.CODEC.fieldOf("scale").forGetter(NoiseBlockFunction::scale),
      WeightedList.createMapCodec(BlockFunction.CODEC).fieldOf("list").forGetter(NoiseBlockFunction::list)
  ).apply(instance, NoiseBlockFunction::new));

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
      return source -> new NoiseBlockFunction(seed, new DoublePerlinNoiseSampler.NoiseParameters(firstOctave, amplitudes), scale, weightedList.transform(blockFunctionArgument -> blockFunctionArgument.apply(source)));
    }
  }
}
