package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.predicate.block.BlockPredicate;

/**
 * 当原先的方块符合方块谓词时，应用函数 1，否则应用函数 2。例如：
 * <blockquote>
 * <code>if(#air, stone, water)</code> - 如果原先的方块是空气，则产生石头，否则产生水。
 * </blockquote>
 */
public record ConditionalBlockFunction(@NotNull BlockPredicate condition, @NotNull BlockFunction functionIfTrue, @NotNull BlockFunction functionIfFalse) implements BlockFunction {
  public static final MapCodec<ConditionalBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply3(ConditionalBlockFunction::new, BlockPredicate.CODEC.fieldOf("condition").forGetter(ConditionalBlockFunction::condition), BlockFunction.CODEC.fieldOf("then").forGetter(ConditionalBlockFunction::functionIfTrue), BlockFunction.CODEC.optionalFieldOf("else", EmptyBlockFunction.INSTANCE).forGetter(ConditionalBlockFunction::functionIfFalse)));

  public ConditionalBlockFunction(@NotNull BlockPredicate condition, @NotNull BlockFunction functionIfTrue) {
    this(condition, functionIfTrue, EmptyBlockFunction.INSTANCE);
  }

  @Override
  public @NotNull String asString() {
    return "if(" + condition.asString() + ", " + functionIfTrue.asString() + (functionIfFalse == EmptyBlockFunction.INSTANCE ? "" : ", " + functionIfFalse.asString()) + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, Level world, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
    final BlockInWorld cachedBlockPosition = new BlockInWorld(world, pos, false);
    if (condition.test(cachedBlockPosition, context)) {
      return functionIfTrue.getModifiedState(blockState, origState, world, pos, blockEntityData, context);
    } else {
      return functionIfFalse.getModifiedState(blockState, origState, world, pos, blockEntityData, context);
    }
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.CONDITIONAL;
  }

  public enum Type implements BlockFunctionType<ConditionalBlockFunction> {
    CONDITIONAL_TYPE;

    @Override
    public @NotNull MapCodec<ConditionalBlockFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionLikeParser.SequentialParams<ConditionalBlockFunction> {
    private BlockPredicate condition;
    private BlockFunction valueIfTrue, valueIfFalse;

    @Override
    public ConditionalBlockFunction getParseResult(ParseContext<?> parseContext) {
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
