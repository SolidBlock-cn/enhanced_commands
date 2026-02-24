package pers.solid.ecmd.predicate.block;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;

import java.util.ArrayList;
import java.util.List;

public record AllBlockPredicate(List<@NotNull BlockPredicate> predicates) implements BlockPredicate {
  public static final MapCodec<AllBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockPredicate.CODEC.listOf().fieldOf("predicates").forGetter(AllBlockPredicate::predicates)).apply(i, AllBlockPredicate::new));

  @Override
  public @NotNull String asString() {
    return "all(" + String.join(", ", Collections2.transform(predicates, ExpressionConvertible::asString)) + ")";
  }

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    return predicates.stream().allMatch(blockPredicate -> blockPredicate.test(blockInWorld, executionContext));
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final ImmutableList.Builder<TestResult> results = new ImmutableList.Builder<>();
    int successes = 0;
    for (BlockPredicate blockPredicate : predicates) {
      TestResult testResult = blockPredicate.testAndDescribe(blockInWorld, executionContext);
      results.add(testResult);
      if (testResult.successes())
        successes++;
    }
    final ImmutableList<TestResult> build = results.build();
    if (successes < build.size()) {
      return TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.all.fail", successes, build.size()), build);
    } else {
      return TestResult.of(true, Component.translatable("enhanced_commands.block_predicate.all.pass", successes, build.size()), build);
    }
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.ALL;
  }

  public enum Type implements BlockPredicateType<AllBlockPredicate> {
    ALL_TYPE;

    @Override
    public @NotNull MapCodec<AllBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  public record Parser(List<BlockPredicate> blockPredicates) implements FunctionLikeParser.SequentialParams<AllBlockPredicate> {
    public Parser() {
      this(new ArrayList<>());
    }

    @Override
    public AllBlockPredicate getParseResult(ParseContext<?> parseContext) {
      return new AllBlockPredicate(blockPredicates);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      blockPredicates.add(BlockPredicate.parse(parseContext));
    }
  }
}
