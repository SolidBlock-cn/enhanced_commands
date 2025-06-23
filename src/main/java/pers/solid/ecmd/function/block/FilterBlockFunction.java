package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParseContext;

/**
 * 一种特殊的方块函数，当指定的方块函数的结果只有符合指定的谓词时，才会应用。例如：
 * <pre>
 *   filter(*, !#infiniburn)  // 任何方块，但不能用 #infiniburn 标签，否则不应用。
 *   filter(*, !#infiniburn, bedrock)  // 任何方块，如果其随机的结果是含有 #infiniburn 标签的方块，则使用基岩。
 * </pre>
 * 注意：此方法不一定能够正常地对方块实体进行检测。
 */
public record FilterBlockFunction(@NotNull BlockFunction function, @NotNull BlockPredicate predicate, @NotNull BlockFunction elseFunction) implements BlockFunction {
  public static final MapCodec<FilterBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply3(FilterBlockFunction::new, BlockFunction.CODEC.fieldOf("function").forGetter(FilterBlockFunction::function), BlockPredicate.CODEC.fieldOf("predicate").forGetter(FilterBlockFunction::predicate), BlockFunction.CODEC.optionalFieldOf("else", EmptyBlockFunction.INSTANCE).forGetter(FilterBlockFunction::elseFunction)));

  @Override
  public @NotNull String asString() {
    return "filter(" + function.asString() + ", " + predicate.asString() + (elseFunction.isEmpty() ? "" : ", " + elseFunction.asString()) + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    final NbtCompound valueBeforeModify = blockEntityData.getValue();
    final BlockState newState = function.getModifiedState(blockState, origState, world, pos, blockEntityData, context);
    final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, pos, false);
    if (predicate.test(cachedBlockPosition, context)) {
      return newState;
    } else {
      blockEntityData.setValue(valueBeforeModify);
      return elseFunction.getModifiedState(blockState, origState, world, pos, blockEntityData, context);
    }
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.FILTER;
  }

  public enum Type implements BlockFunctionType<FilterBlockFunction> {
    FILTER_TYPE;

    @Override
    public @NotNull MapCodec<FilterBlockFunction> getCodec() {
      return CODEC;
    }
  }

  public static final class Parser implements FunctionParamsParser<FilterBlockFunction> {
    private BlockPredicate blockPredicate;
    private BlockFunction blockFunction;
    private @Nullable BlockFunction elseFunction;

    @Override
    public FilterBlockFunction getParseResult(ParseContext<?> parseContext) {
      return new FilterBlockFunction(blockFunction, blockPredicate, elseFunction == null ? EmptyBlockFunction.INSTANCE : elseFunction);
    }

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      if (paramIndex == 0) {
        blockFunction = BlockFunction.parse(parseContext);
      } else if (paramIndex == 1) {
        blockPredicate = BlockPredicate.parse(parseContext);
      } else if (paramIndex == 2) {
        elseFunction = BlockFunction.parse(parseContext);
      }
    }

    // @formatter:off
    @Override public int minParamsCount() {return 2;}
    @Override public int maxParamsCount() {return 3;}
    // @formatter:on
  }
}
