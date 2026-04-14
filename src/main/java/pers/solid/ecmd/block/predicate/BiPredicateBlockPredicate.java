package pers.solid.ecmd.block.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

import java.util.List;

public record BiPredicateBlockPredicate(BlockPredicate blockPredicate1, BlockPredicate blockPredicate2, boolean same) implements BlockPredicate {
  public static final MapCodec<BiPredicateBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockPredicate.CODEC.fieldOf("block_predicate1").forGetter(BiPredicateBlockPredicate::blockPredicate1), BlockPredicate.CODEC.fieldOf("block_predicate2").forGetter(BiPredicateBlockPredicate::blockPredicate2), Codec.BOOL.fieldOf("same").forGetter(BiPredicateBlockPredicate::same)).apply(i, BiPredicateBlockPredicate::new));

  @Override
  public @NotNull String asString() {
    return (same ? "same" : "diff") + "(" + blockPredicate1.asString() + ", " + blockPredicate2.asString() + ")";
  }

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    return (blockPredicate1.test(blockInWorld, executionContext) == blockPredicate2.test(blockInWorld, executionContext)) == same;
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final TestResult testResult1 = blockPredicate1.testAndDescribe(blockInWorld, executionContext);
    final TestResult testResult2 = blockPredicate2.testAndDescribe(blockInWorld, executionContext);
    final boolean actual = testResult1.successes() == testResult2.successes();
    final boolean result = actual == same;
    final String passOfFail = result ? "pass" : "fail";
    final String sameOrDiff = actual ? "same" : "diff";
    return TestResult.of(result, Component.translatable("enhanced_commands.block_predicate.bi_predicate_" + sameOrDiff + "_" + passOfFail), List.of(testResult1, testResult2));
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.BI_PREDICATE;
  }

  public enum Type implements BlockPredicateType<BiPredicateBlockPredicate> {
    BI_PREDICATE_TYPE;

    @Override
    public @NotNull MapCodec<BiPredicateBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  public static final class Parser implements FunctionContentParser.SequentialParams<BiPredicateBlockPredicate> {
    private final boolean same;
    private BlockPredicate value1;
    private BlockPredicate value2;

    public Parser(boolean same) {
      this.same = same;
    }

    @Override
    public BiPredicateBlockPredicate getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      if (value1 == null) {
        throw SequentialParams.PARAMS_TOO_FEW.createWithContext(parseContext.reader(), 2, 1);
      }
      return new BiPredicateBlockPredicate(value1, value2, same);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final BlockPredicate parse = BlockPredicate.parse(parseContext);
      if (value1 == null) {
        value1 = parse;
      } else if (value2 == null) {
        value2 = parse;
      } else {
        throw SequentialParams.PARAMS_TOO_MANY.createWithContext(parseContext.reader(), 2, 3);
      }
    }
  }
}
