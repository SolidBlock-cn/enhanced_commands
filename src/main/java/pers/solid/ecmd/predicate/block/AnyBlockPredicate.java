package pers.solid.ecmd.predicate.block;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.ArrayList;
import java.util.List;

public record AnyBlockPredicate(List<BlockPredicate> predicates) implements BlockPredicate {
  public static final MapCodec<AnyBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockPredicate.CODEC.listOf().fieldOf("predicates").forGetter(AnyBlockPredicate::predicates)).apply(i, AnyBlockPredicate::new));

  public AnyBlockPredicate(BlockPredicate... predicates) {
    this(List.of(predicates));
  }

  @Override
  public @NotNull String asString() {
    return "any(" + String.join(", ", Collections2.transform(predicates, ExpressionConvertible::asString)) + ")";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    return predicates.stream().anyMatch(blockPredicate -> blockPredicate.test(cachedBlockPosition, context));
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final ImmutableList.Builder<TestResult> results = new ImmutableList.Builder<>();
    int successes = 0;
    for (BlockPredicate blockPredicate : predicates) {
      TestResult testResult = blockPredicate.testAndDescribe(cachedBlockPosition, context);
      results.add(testResult);
      if (testResult.successes())
        successes++;
    }
    final ImmutableList<TestResult> build = results.build();
    if (successes > 0) {
      return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.any.pass", successes, build.size()), build);
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.any.fail", successes, build.size()), build);
    }
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.ANY;
  }

  public enum Type implements BlockPredicateType<AnyBlockPredicate> {
    ANY_TYPE;

    @Override
    public @NotNull MapCodec<AnyBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  public record Parser(List<BlockPredicate> blockPredicates) implements FunctionParamsParser<AnyBlockPredicate> {
    public Parser() {
      this(new ArrayList<>());
    }

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final BlockPredicate parse = BlockPredicate.parse(parseContext);
      blockPredicates.add(parse);
    }

    @Override
    public AnyBlockPredicate getParseResult(ParseContext<?> parseContext) {
      return new AnyBlockPredicate(blockPredicates);
    }
  }
}
