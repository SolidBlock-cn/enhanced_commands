package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.Checkerboard;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.parse.ParseContext;

public record CheckerboardBlockFunction(@NotNull WeightedList<BlockFunction> functions, @NotNull Vec3d floor, @NotNull Vec3d scale, @NotNull Vec3d offset) implements BlockFunction, Checkerboard<BlockFunction> {
  public static final MapCodec<CheckerboardBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      WeightedList.createMapCodec(BlockFunction.CODEC).fieldOf("predicates").forGetter(CheckerboardBlockFunction::functions),
      Vec3d.CODEC.optionalFieldOf("floor", Vec3d.ZERO).forGetter(CheckerboardBlockFunction::floor),
      Vec3d.CODEC.optionalFieldOf("scale", UNIT).forGetter(CheckerboardBlockFunction::scale),
      Vec3d.CODEC.optionalFieldOf("offset", Vec3d.ZERO).forGetter(CheckerboardBlockFunction::offset)
  ).apply(i, CheckerboardBlockFunction::new));

  public CheckerboardBlockFunction(@NotNull WeightedList<BlockFunction> functions) {
    this(functions, Vec3d.ZERO, UNIT, Vec3d.ZERO);
  }

  @Override
  @NotNull
  public Type getType() {
    return BlockFunctionTypes.CHECKERBOARD;
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    final BlockFunction entry = getEntry(functions, pos);
    return entry == null ? blockState : entry.getModifiedState(blockState, origState, world, pos, blockEntityData, context);
  }

  @Override
  public @NotNull String asString() {
    final StringBuilder sb = new StringBuilder("checkerboard");
    sb.append(functions.asString(ExpressionConvertible::asString));
    appendParameters(sb);
    return sb.append(")").toString();
  }

  public enum Type implements BlockFunctionType<CheckerboardBlockFunction> {
    CHECKERBOARD_TYPE;

    @Override
    public @NotNull MapCodec<CheckerboardBlockFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser extends CheckerboardParser<BlockFunction> {
    @Override
    protected CheckerboardBlockFunction getParseResult(Vec3d floor, Vec3d scale, Vec3d offset) {
      return new CheckerboardBlockFunction(weightedList.transform(blockFunctionArgument -> blockFunctionArgument), floor, scale, offset);
    }

    @Override
    protected BlockFunction parseElement(ParseContext<?> parseContext) throws CommandSyntaxException {
      return BlockFunctionArgument.parse(parseContext);
    }
  }
}
