package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.Noise;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.OptionalLong;

public record NoiseBlockFunction(WeightedList<BlockFunction> list, Properties properties) implements BlockFunction, Noise {
  public static final MapCodec<NoiseBlockFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      WeightedList.createMapCodec(BlockFunction.CODEC).fieldOf("list").forGetter(NoiseBlockFunction::list),
      CodecUtil.optionalLongFieldOf("seed").forGetter(NoiseBlockFunction::seed),
      DoublePerlinNoiseSampler.NoiseParameters.CODEC.fieldOf("parameters").forGetter(NoiseBlockFunction::noiseParameters),
      Vec3d.CODEC.optionalFieldOf("scale", UNIT).forGetter(NoiseBlockFunction::scale),
      Vec3d.CODEC.optionalFieldOf("offset", Vec3d.ZERO).forGetter(NoiseBlockFunction::offset)
  ).apply(instance, NoiseBlockFunction::new));

  public NoiseBlockFunction(WeightedList<BlockFunction> list, OptionalLong seed, DoublePerlinNoiseSampler.NoiseParameters noiseParameters, Vec3d scale, Vec3d offset) {
    this(list, new Properties(seed, noiseParameters, scale, offset));
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    return sample(seed().orElseGet(() -> context.getSeed(this)), list, Vec3d.of(pos)).getModifiedState(blockState, origState, world, pos, blockEntityData, context);
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
    protected BlockFunctionArgument parseElement(ParseContext<?> parseContext) throws CommandSyntaxException {
      return BlockFunctionArgument.parse(parseContext.withAllowSparse(true));
    }

    @Override
    public BlockFunctionArgument getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      super.getParseResult(parseContext);
      return source -> new NoiseBlockFunction(weightedList.transform(blockFunctionArgument -> blockFunctionArgument.apply(source)), seed, new DoublePerlinNoiseSampler.NoiseParameters(firstOctave, amplitudes), scale, offset);
    }
  }
}
