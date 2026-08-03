package pers.solid.ecmd.block.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.math.Noise;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.pack.RequiresValidation;

import java.util.OptionalLong;

public record NoiseBlockFunction(WeightedList<BlockFunction> list, Properties properties) implements BlockFunction, Noise {
  public static final MapCodec<NoiseBlockFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      WeightedList.createMapCodec(BlockFunction.CODEC).fieldOf("list").forGetter(NoiseBlockFunction::list),
      CodecUtil.optionalLongFieldOf("seed").forGetter(NoiseBlockFunction::seed),
      NormalNoise.NoiseParameters.DIRECT_CODEC.fieldOf("parameters").forGetter(NoiseBlockFunction::noiseParameters),
      Vec3.CODEC.optionalFieldOf("scale", UNIT).forGetter(NoiseBlockFunction::scale),
      Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(NoiseBlockFunction::offset)
  ).apply(instance, NoiseBlockFunction::new));

  public NoiseBlockFunction(WeightedList<BlockFunction> list, OptionalLong seed, NormalNoise.NoiseParameters noiseParameters, Vec3 scale, Vec3 offset) {
    this(list, new Properties(seed, noiseParameters, scale, offset));
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) throws CommandSyntaxException {
    return sample(seed().orElseGet(() -> context.getSeed(this)), list, Vec3.atLowerCornerOf(pos)).getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
  }

  @Override
  public BlockFunctionType<NoiseBlockFunction> getType() {
    return BlockFunctionTypes.NOISE;
  }

  @Override
  public String expressAsString() {
    final String listString = list.asString(ExpressionConvertible::expressAsString);
    final String parametersString = asParametersString();
    return "noise(" + listString + (parametersString.isEmpty() ? "" : "; " + parametersString) + ")";
  }

  @Override
  public Iterable<? extends RequiresValidation> membersToValidate() {
    return list;
  }

  public static class Parser extends Noise.Parser<BlockFunction> {

    @Override
    protected BlockFunction parseElement(ParseContext<?> parseContext) throws CommandSyntaxException {
      return BlockFunction.parse(parseContext.withAllowSparse(true));
    }

    @Override
    public BlockFunction getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      super.getParseResult(parseContext);
      return new NoiseBlockFunction(weightedList, seed, new NormalNoise.NoiseParameters(firstOctave, amplitudes), scale, offset);
    }
  }
}
