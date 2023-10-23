package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.mixin.CachedBlockPositionAccessor;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.BlockPredicateArgument;
import pers.solid.ecmd.util.FunctionLikeParser;

/**
 * 一种特殊的方块函数，当指定的方块函数的结果只有符合指定的谓词时，才会应用。例如：
 * <pre>
 *   filter(*, !#infiniburn)  // 任何方块，但不能用 #infiniburn 标签，否则不应用。
 *   filter(*, !#infiniburn, bedrock)  // 任何方块，如果其随机的结果是含有 #infiniburn 标签的方块，则使用基岩。
 * </pre>
 * 注意：此方法不一定能够正常地对方块实体进行检测。
 */
public record FilterBlockFunction(@NotNull BlockFunction blockFunction, @NotNull BlockPredicate blockPredicate, @Nullable BlockFunction elseFunction) implements BlockFunction {
  @Override
  public @NotNull String asString() {
    return "filter(" + blockFunction.asString() + ", " + blockPredicate.asString() + (elseFunction == null ? "" : ", " + elseFunction.asString()) + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    final NbtCompound valueBeforeModify = blockEntityData.getValue();
    final BlockState newState = blockFunction.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, pos, false);
    ((CachedBlockPositionAccessor) cachedBlockPosition).setState(newState);
    if (blockPredicate.test(cachedBlockPosition)) {
      return newState;
    } else if (elseFunction == null) {
      blockEntityData.setValue(valueBeforeModify);
      return blockState;
    } else {
      blockEntityData.setValue(valueBeforeModify);
      return elseFunction.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    }
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.put("function", blockFunction.createNbt());
    nbtCompound.put("predicate", blockPredicate.createNbt());
    if (elseFunction != null) {
      nbtCompound.put("else", elseFunction.createNbt());
    }
  }

  @Override
  public @NotNull BlockFunctionType<FilterBlockFunction> getType() {
    return BlockFunctionTypes.FILTER;
  }

  public enum Type implements BlockFunctionType<FilterBlockFunction> {
    FILTER_TYPE;

    @Override
    public @NotNull FilterBlockFunction fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new FilterBlockFunction(
          BlockFunction.fromNbt(nbtCompound.getCompound("function"), world), BlockPredicate.fromNbt(nbtCompound.getCompound("predicate"), world),
          nbtCompound.contains("else", NbtElement.COMPOUND_TYPE) ? BlockFunction.fromNbt(nbtCompound.getCompound("else"), world) : null
      );
    }

    @Override
    public @Nullable BlockFunctionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  private static final class Parser implements FunctionLikeParser<BlockFunctionArgument> {
    private BlockPredicateArgument blockPredicate;
    private BlockFunctionArgument blockFunction;
    private @Nullable BlockFunctionArgument elseFunction;

    @Override
    public @NotNull String functionName() {
      return "filter";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.block_function.filter");
    }

    @Override
    public BlockFunctionArgument getParseResult(SuggestedParser parser) {
      return source -> new FilterBlockFunction(blockFunction.apply(source), blockPredicate.apply(source), elseFunction == null ? null : elseFunction.apply(source));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        blockFunction = BlockFunctionArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
      } else if (paramIndex == 1) {
        blockPredicate = BlockPredicateArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
      } else if (paramIndex == 2) {
        elseFunction = BlockFunctionArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
      }
    }

    // @formatter:off
    @Override public int minParamsCount() {return 2;}
    @Override public int maxParamsCount() {return 3;}
    // @formatter:on
  }
}
