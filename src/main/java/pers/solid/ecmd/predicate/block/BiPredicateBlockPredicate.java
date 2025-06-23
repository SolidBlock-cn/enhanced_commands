package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.List;

public record BiPredicateBlockPredicate(BlockPredicate blockPredicate1, BlockPredicate blockPredicate2, boolean same) implements BlockPredicate {
  public static final MapCodec<BiPredicateBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockPredicate.CODEC.fieldOf("block_predicate1").forGetter(BiPredicateBlockPredicate::blockPredicate1), BlockPredicate.CODEC.fieldOf("block_predicate2").forGetter(BiPredicateBlockPredicate::blockPredicate2), Codec.BOOL.fieldOf("same").forGetter(BiPredicateBlockPredicate::same)).apply(i, BiPredicateBlockPredicate::new));

  @Override
  public @NotNull String asString() {
    return (same ? "same" : "diff") + "(" + blockPredicate1.asString() + ", " + blockPredicate2.asString() + ")";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    return (blockPredicate1.test(cachedBlockPosition, context) == blockPredicate2.test(cachedBlockPosition, context)) == same;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final TestResult testResult1 = blockPredicate1.testAndDescribe(cachedBlockPosition, context);
    final TestResult testResult2 = blockPredicate2.testAndDescribe(cachedBlockPosition, context);
    final boolean actual = testResult1.successes() == testResult2.successes();
    final boolean result = actual == same;
    final String passOfFail = result ? "pass" : "fail";
    final String sameOrDiff = actual ? "same" : "diff";
    return TestResult.of(result, Text.translatable("enhanced_commands.block_predicate.bi_predicate_" + sameOrDiff + "_" + passOfFail), List.of(testResult1, testResult2));
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

  public static final class Parser implements FunctionParamsParser<BiPredicateBlockPredicate> {
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
    public BiPredicateBlockPredicate getParseResult(ParseContext<?> parseContext) {
      return new BiPredicateBlockPredicate(value1, value2, same);
    }

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final BlockPredicate parse = BlockPredicate.parse(parseContext);
      if (value1 == null) {
        value1 = parse;
      } else if (value2 == null) {
        value2 = parse;
      } else {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parseContext.reader());
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
}
