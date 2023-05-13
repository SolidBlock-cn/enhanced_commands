package pers.solid.mod.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mod.argument.SuggestedParser;
import pers.solid.mod.command.TestResult;

import java.util.List;

public record BiPredicateBlockPredicate(BlockPredicate blockPredicate1, BlockPredicate blockPredicate2, boolean same) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    return (same ? "same" : "diff") + "(" + blockPredicate1 + ", " + blockPredicate2 + ")";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return (blockPredicate1.test(cachedBlockPosition) == blockPredicate2.test(cachedBlockPosition)) == same;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final TestResult testResult1 = blockPredicate1.testAndDescribe(cachedBlockPosition);
    final TestResult testResult2 = blockPredicate2.testAndDescribe(cachedBlockPosition);
    final boolean actual = testResult1.successes() == testResult2.successes();
    final boolean result = actual == same;
    final String passOfFail = result ? "pass" : "fail";
    final String sameOrDiff = actual ? "same" : "diff";
    return new TestResult(result, List.of(Text.translatable("blockPredicate.bi_predicate_" + sameOrDiff + "_" + passOfFail).formatted(result ? Formatting.GREEN : Formatting.RED)), List.of(testResult1, testResult2));
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.BI_PREDICATE;
  }

  public static final class Parser implements FunctionLikeParser<BiPredicateBlockPredicate> {
    private final String functionName;
    private final Text tooltip;
    private final boolean same;
    private BlockPredicate value1;
    private BlockPredicate value2;

    public Parser(String functionName, Text tooltip, boolean same) {
      this.functionName = functionName;
      this.tooltip = tooltip;
      this.same = same;
    }

    @Override
    public @NotNull String functionName() {
      return functionName;
    }

    @Override
    public Text tooltip() {
      return tooltip;
    }

    @Override
    public BiPredicateBlockPredicate getParseResult() {
      return new BiPredicateBlockPredicate(value1, value2, same);
    }

    @Override
    public void parseParameter(SuggestedParser parser, int paramIndex) throws CommandSyntaxException {
      final BlockPredicate parse = BlockPredicate.parse(parser);
      if (value1 == null) {
        value1 = parse;
      } else if (value2 == null) {
        value2 = parse;
      } else {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parser.reader);
      }
    }

    @Override
    public int minParamsCount() {
      return 2;
    }

    @Override
    public int maxParamsCount() {
      return 2;
    }
  }

  public enum Type implements BlockPredicateType<BiPredicateBlockPredicate> {
    INSTANCE;

    @Override
    public @Nullable BlockPredicate parse(SuggestedParser parser) throws CommandSyntaxException {
      CommandSyntaxException exception = null;
      try {
        final BiPredicateBlockPredicate parse1 = new Parser("same", Text.translatable("blockPredicate.bi_predicate_same"), true).parse(parser);
        if (parse1 != null) {
          return parse1;
        }
      } catch (CommandSyntaxException exception1) {
        exception = exception1;
      }
      final BiPredicateBlockPredicate parse2 = new Parser("diff", Text.translatable("blockPredicate.bi_predicate_diff"), false).parse(parser);
      if (parse2 != null) {
        return parse2;
      } else if (exception != null) {
        throw exception;
      } else {
        return null;
      }
    }
  }
}
