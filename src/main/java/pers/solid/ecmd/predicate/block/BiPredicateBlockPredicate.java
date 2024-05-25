package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.FunctionParamsParser;

import java.util.List;

public record BiPredicateBlockPredicate(BlockPredicate blockPredicate1, BlockPredicate blockPredicate2, boolean same) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    return (same ? "same" : "diff") + "(" + blockPredicate1.asString() + ", " + blockPredicate2.asString() + ")";
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
    return TestResult.of(result, Text.translatable("enhanced_commands.block_predicate.bi_predicate_" + sameOrDiff + "_" + passOfFail), List.of(testResult1, testResult2));
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.BI_PREDICATE;
  }

  public static final class Parser implements FunctionParamsParser<BlockPredicateArgument> {
    private final String functionName;
    private final Text tooltip;
    private final boolean same;
    private BlockPredicateArgument value1;
    private BlockPredicateArgument value2;

    public Parser(String functionName, Text tooltip, boolean same) {
      this.functionName = functionName;
      this.tooltip = tooltip;
      this.same = same;
    }

    @Override
    public BlockPredicateArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) {
      return source -> new BiPredicateBlockPredicate(value1.apply(source), value2.apply(source), same);
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final BlockPredicateArgument parse = BlockPredicateArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
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

  public static final Codec<BiPredicateBlockPredicate> CODEC = RecordCodecBuilder.create(i -> i.group(BlockPredicate.CODEC.fieldOf("block_predicate1").forGetter(BiPredicateBlockPredicate::blockPredicate1), BlockPredicate.CODEC.fieldOf("block_predicate2").forGetter(BiPredicateBlockPredicate::blockPredicate2), Codec.BOOL.fieldOf("same").forGetter(BiPredicateBlockPredicate::same)).apply(i, BiPredicateBlockPredicate::new));

  public enum Type implements BlockPredicateType<BiPredicateBlockPredicate> {
    BI_PREDICATE_TYPE;

    @Override
    public @NotNull Codec<BiPredicateBlockPredicate> getCodec() {
      return CODEC;
    }
  }
}
