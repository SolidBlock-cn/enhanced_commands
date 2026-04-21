package pers.solid.ecmd.block.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;

/**
 * 去除方块函数中的流体，并将 waterlogged 设为 false。这不一定总是能够成功。
 */
public record DryBlockFunction(BlockFunction function) implements BlockFunction {
  public static final MapCodec<DryBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockFunction.CODEC.optionalFieldOf("function", EmptyBlockFunction.INSTANCE).forGetter(DryBlockFunction::function)).apply(i, DryBlockFunction::new));

  @Override
  public String expressAsString() {
    return "dry(" + (function.isEmpty() ? "" : function.expressAsString()) + ")";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) {
    BlockState state = function.getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
    if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
      state = state.setValue(BlockStateProperties.WATERLOGGED, false);
    }
    if (!state.getFluidState().isEmpty()) {
      return Blocks.AIR.defaultBlockState();
    }
    return state;
  }

  @Override
  public BlockFunctionType<DryBlockFunction> getType() {
    return BlockFunctionTypes.DRY;
  }

  public static class Parser implements FunctionContentParser.SequentialParams<DryBlockFunction> {
    private @Nullable BlockFunction blockFunction = null;

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
