package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.Checkerboard;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExpressionConvertible;

public record CheckerboardBlockFunction(@NotNull WeightedList<BlockFunction> functions, @NotNull Vec3 floor, @NotNull Vec3 scale, @NotNull Vec3 offset) implements BlockFunction, Checkerboard<BlockFunction> {
  public static final MapCodec<CheckerboardBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      WeightedList.createMapCodec(BlockFunction.CODEC).fieldOf("predicates").forGetter(CheckerboardBlockFunction::functions),
      Vec3.CODEC.optionalFieldOf("floor", Vec3.ZERO).forGetter(CheckerboardBlockFunction::floor),
      Vec3.CODEC.optionalFieldOf("scale", UNIT).forGetter(CheckerboardBlockFunction::scale),
      Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(CheckerboardBlockFunction::offset)
  ).apply(i, CheckerboardBlockFunction::new));

  public CheckerboardBlockFunction(@NotNull WeightedList<BlockFunction> functions) {
    this(functions, Vec3.ZERO, UNIT, Vec3.ZERO);
  }

  @Override
  @NotNull
  public Type getType() {
    return BlockFunctionTypes.CHECKERBOARD;
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, Level world, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
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
    protected CheckerboardBlockFunction getParseResult(Vec3 floor, Vec3 scale, Vec3 offset) {
      return new CheckerboardBlockFunction(weightedList.transform(blockFunctionArgument -> blockFunctionArgument), floor, scale, offset);
    }

    @Override
    protected BlockFunction parseElement(ParseContext<?> parseContext) throws CommandSyntaxException {
      return BlockFunction.parse(parseContext);
    }
  }
}
