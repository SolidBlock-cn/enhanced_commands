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
 * 当原先的方块符合方块谓词时，应用函数 1，否则应用函数 2。例如：
 * <blockquote>
 * <code>if(#air, stone, water)</code> - 如果原先的方块是空气，则产生石头，否则产生水。
 * </blockquote>
 */
public record ConditionalBlockFunction(BlockPredicate condition, BlockFunction functionIfTrue, BlockFunction functionIfFalse) implements BlockFunction {
  public static final MapCodec<ConditionalBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply3(ConditionalBlockFunction::new, BlockPredicate.CODEC.fieldOf("condition").forGetter(ConditionalBlockFunction::condition), BlockFunction.CODEC.fieldOf("then").forGetter(ConditionalBlockFunction::functionIfTrue), BlockFunction.CODEC.optionalFieldOf("else", EmptyBlockFunction.INSTANCE).forGetter(ConditionalBlockFunction::functionIfFalse)));

  public ConditionalBlockFunction(BlockPredicate condition, BlockFunction functionIfTrue) {
    this(condition, functionIfTrue, EmptyBlockFunction.INSTANCE);
  }

  @Override
  public String expressAsString() {
    return "if(" + condition.expressAsString() + ", " + functionIfTrue.expressAsString() + (functionIfFalse == EmptyBlockFunction.INSTANCE ? "" : ", " + functionIfFalse.expressAsString()) + ")";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, MutableObject<@Nullable CompoundTag> blockEntityData, ExecutionContext context) throws CommandSyntaxException {
    final BlockInWorld blockInWorld = new BlockInWorld(level, pos, false);
    if (condition.test(blockInWorld, context)) {
      return functionIfTrue.getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
    } else {
      return functionIfFalse.getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
    }
  }

  @Override
  public BlockFunctionType<ConditionalBlockFunction> getType() {
    return BlockFunctionTypes.CONDITIONAL;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(condition, functionIfTrue, functionIfFalse);
  }

  public static class Parser implements FunctionContentParser.SequentialParams<ConditionalBlockFunction> {
    private @Nullable BlockPredicate condition;
    private @Nullable BlockFunction valueIfTrue, valueIfFalse;

    @Override
    public ConditionalBlockFunction getParseResult(ParseContext<?> parseContext) {
      Objects.requireNonNull(condition, "condition");
      Objects.requireNonNull(valueIfTrue, "valueIfTrue");
      return new ConditionalBlockFunction(condition, valueIfTrue, valueIfFalse == null ? EmptyBlockFunction.INSTANCE : valueIfFalse);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      if (paramIndex == 0) {
        condition = BlockPredicate.parse(parseContext);
      } else if (paramIndex == 1) {
        valueIfTrue = BlockFunction.parse(parseContext);
      } else if (paramIndex == 2) {
        valueIfFalse = BlockFunction.parse(parseContext);
      }
    }

    @Override
    public int minSequentialParamsCount() {
      return 2;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 3;
    }
  }
}
