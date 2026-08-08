package pers.solid.ecmd.block.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;
import java.util.Objects;

/**
 * 一种特殊的方块函数，当指定的方块函数的结果只有符合指定的谓词时，才会应用。例如：
 * <pre>
 *   filter(*, !#infiniburn)  // 任何方块，但不能用 #infiniburn 标签，否则不应用。
 *   filter(*, !#infiniburn, bedrock)  // 任何方块，如果其随机的结果是含有 #infiniburn 标签的方块，则使用基岩。
 * </pre>
 * 注意：此方法不一定能够正常地对方块实体进行检测。
 */
public record FilterBlockFunction(BlockFunction function, BlockPredicate predicate, BlockFunction elseFunction) implements BlockFunction {
  public static final MapCodec<FilterBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply3(FilterBlockFunction::new, BlockFunction.CODEC.fieldOf("function").forGetter(FilterBlockFunction::function), BlockPredicate.CODEC.fieldOf("predicate").forGetter(FilterBlockFunction::predicate), BlockFunction.CODEC.optionalFieldOf("else", EmptyBlockFunction.INSTANCE).forGetter(FilterBlockFunction::elseFunction)));

  @Override
  public String expressAsString() {
    return "filter(" + function.expressAsString() + ", " + predicate.expressAsString() + (elseFunction.isEmpty() ? "" : ", " + elseFunction.expressAsString()) + ")";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, MutableObject<@Nullable CompoundTag> blockEntityData, ExecutionContext context) throws CommandSyntaxException {
    final CompoundTag valueBeforeModify = blockEntityData.getValue();
    final BlockState newState = function.getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
    final BlockInWorld blockInWorld = new BlockInWorld(level, pos, false);
    if (predicate.test(blockInWorld, context)) {
      return newState;
    } else {
      blockEntityData.setValue(valueBeforeModify);
      return elseFunction.getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
    }
  }

  @Override
  public BlockFunctionType<FilterBlockFunction> getType() {
    return BlockFunctionTypes.FILTER;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(function, predicate, elseFunction);
  }

  public static final class Parser implements FunctionContentParser.SequentialParams<FilterBlockFunction> {
    private @Nullable BlockPredicate blockPredicate;
    private @Nullable BlockFunction blockFunction;
    private @Nullable BlockFunction elseFunction;

    @Override
    public FilterBlockFunction getParseResult(ParseContext<?> parseContext) {
      return new FilterBlockFunction(Objects.requireNonNull(blockFunction, "blockFunction"), Objects.requireNonNull(blockPredicate, "blockPredicate"), elseFunction == null ? EmptyBlockFunction.INSTANCE : elseFunction);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      if (paramIndex == 0) {
        blockFunction = BlockFunction.parse(parseContext);
      } else if (paramIndex == 1) {
        blockPredicate = BlockPredicate.parse(parseContext);
      } else if (paramIndex == 2) {
        elseFunction = BlockFunction.parse(parseContext);
      }
    }

    // @formatter:off
    @Override public int minSequentialParamsCount() {return 2;}
    @Override public int maxSequentialParamsCount() {return 3;}
    // @formatter:on
  }
}
