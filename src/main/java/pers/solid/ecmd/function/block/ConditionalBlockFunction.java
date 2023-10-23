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
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.BlockPredicateArgument;
import pers.solid.ecmd.util.FunctionLikeParser;

/**
 * 当原先的方块符合方块谓词时，应用函数 1，否则应用函数 2。例如：
 * <blockquote>
 * <code>if(#air, stone, water)</code> - 如果原先的方块是空气，则产生石头，否则产生水。
 * </blockquote>
 */
public record ConditionalBlockFunction(@NotNull BlockPredicate condition, @NotNull BlockFunction functionIfTrue, @Nullable BlockFunction functionIfFalse) implements BlockFunction {
  @Override
  public @NotNull String asString() {
    return "if(" + condition.asString() + ", " + functionIfTrue.asString() + (functionIfFalse == null ? "" : ", " + functionIfFalse.asString()) + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, pos, false);
    if (condition.test(cachedBlockPosition)) {
      return functionIfTrue.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    } else if (functionIfFalse != null) {
      return functionIfFalse.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    } else {
      return blockState;
    }
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.put("if", condition.createNbt());
    nbtCompound.put("then", functionIfTrue.createNbt());
    if (functionIfFalse != null) {
      nbtCompound.put("else", functionIfFalse.createNbt());
    }
  }

  @Override
  public @NotNull BlockFunctionType<ConditionalBlockFunction> getType() {
    return BlockFunctionTypes.CONDITIONAL;
  }

  public enum Type implements BlockFunctionType<ConditionalBlockFunction> {
    CONDITIONAL_TYPE;

    @Override
    public @NotNull ConditionalBlockFunction fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new ConditionalBlockFunction(
          BlockPredicate.fromNbt(nbtCompound.getCompound("if"), world),
          BlockFunction.fromNbt(nbtCompound.getCompound("then"), world),
          nbtCompound.contains("else", NbtElement.COMPOUND_TYPE) ? BlockFunction.fromNbt(nbtCompound.getCompound("else"), world) : null
      );
    }

    @Override
    public @Nullable BlockFunctionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  private static class Parser implements FunctionLikeParser<BlockFunctionArgument> {
    private BlockPredicateArgument condition;
    private BlockFunctionArgument valueIfTrue, valueIfFalse;

    @Override
    public @NotNull String functionName() {
      return "if";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.block_function.conditional");
    }

    @Override
    public BlockFunctionArgument getParseResult(SuggestedParser parser) {
      return source -> new ConditionalBlockFunction(condition.apply(source), valueIfTrue.apply(source), valueIfFalse == null ? null : valueIfFalse.apply(source));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        condition = BlockPredicateArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
      } else if (paramIndex == 1) {
        valueIfTrue = BlockFunctionArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
      } else if (paramIndex == 2) {
        valueIfFalse = BlockFunctionArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
      }
    }

    @Override
    public int minParamsCount() {
      return 2;
    }

    @Override
    public int maxParamsCount() {
      return 3;
    }
  }
}
