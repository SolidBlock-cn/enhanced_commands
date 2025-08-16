package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;

/**
 * 去除方块函数中的流体，并将 waterlogged 设为 false。这不一定总是能够成功。
 */
public record DryBlockFunction(@NotNull BlockFunction function) implements BlockFunction {
  public static final MapCodec<DryBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockFunction.CODEC.optionalFieldOf("function", EmptyBlockFunction.INSTANCE).forGetter(DryBlockFunction::function)).apply(i, DryBlockFunction::new));

  @Override
  public @NotNull String asString() {
    return "dry(" + (function.isEmpty() ? "" : function.asString()) + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    BlockState state = function.getModifiedState(blockState, origState, world, pos, blockEntityData, context);
    if (state.contains(Properties.WATERLOGGED)) {
      state = state.with(Properties.WATERLOGGED, false);
    }
    if (!state.getFluidState().isEmpty()) {
      return Blocks.AIR.getDefaultState();
    }
    return state;
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.DRY;
  }

  public enum Type implements BlockFunctionType<DryBlockFunction> {
    DRY_TYPE;

    @Override
    public @NotNull MapCodec<DryBlockFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionLikeParser.SequentialParams<DryBlockFunction> {
    BlockFunction blockFunction = null;

    @Override
    public DryBlockFunction getParseResult(ParseContext<?> parseContext) {
      return new DryBlockFunction(blockFunction == null ? EmptyBlockFunction.INSTANCE : blockFunction);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      blockFunction = BlockFunction.parse(parseContext);
    }

    @Override
    public int minSequentialParamsCount() {
      return 0;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 1;
    }
  }
}
