package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.mixin.CachedBlockPositionAccessor;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.util.FunctionLikeParser;

/**
 * 一种特殊的方块函数，当指定的方块函数的结果只有符合指定的谓词时，才会应用。例如：
 * <pre>
 *   filter(*, !#infiniburn)  // 任何方块，但不能用 #infiniburn 标签，否则不应用。
 * </pre>
 * 注意：此方法不一定能够正常地对方块实体进行检测。
 */
public record FilterBlockFunction(@NotNull BlockFunction blockFunction, @NotNull BlockPredicate blockPredicate) implements BlockFunction {
  @Override
  public @NotNull String asString() {
    return "filter(" + blockFunction.asString() + ", " + blockPredicate.asString() + ")";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    final NbtCompound valueBeforeModify = blockEntityData.getValue();
    final BlockState newState = blockFunction.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, pos, false);
    ((CachedBlockPositionAccessor) cachedBlockPosition).setState(newState);
    if (blockPredicate.test(cachedBlockPosition)) {
      return newState;
    } else {
      blockEntityData.setValue(valueBeforeModify);
      return blockState;
    }
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.put("function", blockFunction.createNbt());
    nbtCompound.put("predicate", blockPredicate.createNbt());
  }

  @Override
  public @NotNull BlockFunctionType<FilterBlockFunction> getType() {
    return BlockFunctionTypes.FILTER;
  }

  public enum Type implements BlockFunctionType<FilterBlockFunction> {
    FILTER_TYPE;

    @Override
    public FilterBlockFunction fromNbt(NbtCompound nbtCompound) {
      return new FilterBlockFunction(
          BlockFunction.fromNbt(nbtCompound.getCompound("function")),
          BlockPredicate.fromNbt(nbtCompound.getCompound("predicate"))
      );
    }

    @Override
    public @Nullable FilterBlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  private static final class Parser implements FunctionLikeParser<FilterBlockFunction> {
    private BlockFunction blockFunction;
    private BlockPredicate blockPredicate;

    @Override
    public @NotNull String functionName() {
      return "filter";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.block_function.filter");
    }

    @Override
    public FilterBlockFunction getParseResult(SuggestedParser parser) throws CommandSyntaxException {
      return new FilterBlockFunction(blockFunction, blockPredicate);
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        blockFunction = BlockFunction.parse(commandRegistryAccess, parser, suggestionsOnly);
      } else if (paramIndex == 1) {
        blockPredicate = BlockPredicate.parse(commandRegistryAccess, parser, suggestionsOnly);
      }
    }
  }
}
