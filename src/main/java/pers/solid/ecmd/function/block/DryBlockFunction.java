package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionParamsParser;

/**
 * 去除方块函数中的流体，并将 waterlogged 设为 false。这不一定总是能够成功。
 */
public record DryBlockFunction(@Nullable BlockFunction blockFunction) implements BlockFunction {
  @Override
  public @NotNull String asString() {
    return "dry(" + (blockFunction == null ? "" : blockFunction.asString()) + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    BlockState state = blockFunction == null ? blockState : blockFunction.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    if (state.contains(Properties.WATERLOGGED)) {
      state = state.with(Properties.WATERLOGGED, false);
    }
    if (!state.getFluidState().isEmpty()) {
      return Blocks.AIR.getDefaultState();
    }
    return state;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    if (blockFunction != null) {
      nbtCompound.put("function", blockFunction.createNbt());
    }
  }

  @Override
  public @NotNull BlockFunctionType<DryBlockFunction> getType() {
    return BlockFunctionTypes.DRY;
  }

  public enum Type implements BlockFunctionType<DryBlockFunction> {
    DRY_TYPE;

    @Override
    public @NotNull DryBlockFunction fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      if (nbtCompound.contains("function", NbtElement.COMPOUND_TYPE)) {
        return new DryBlockFunction(BlockFunction.fromNbt(nbtCompound.getCompound("function"), world));
      } else {
        return new DryBlockFunction(null);
      }
    }
  }

  public static class Parser implements FunctionParamsParser<BlockFunctionArgument> {
    BlockFunctionArgument blockFunction = null;

    @Override
    public BlockFunctionArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) {
      return source -> new DryBlockFunction(blockFunction == null ? null : blockFunction.apply(source));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      blockFunction = BlockFunctionArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
    }

    @Override
    public int minParamsCount() {
      return 0;
    }

    @Override
    public int maxParamsCount() {
      return 1;
    }
  }
}
