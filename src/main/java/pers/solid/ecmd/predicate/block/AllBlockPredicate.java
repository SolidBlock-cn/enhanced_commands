package pers.solid.ecmd.predicate.block;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.parse.FunctionParamsParser;

import java.util.ArrayList;
import java.util.List;

public record AllBlockPredicate(List<BlockPredicate> blockPredicates) implements BlockPredicate {
  public static final MapCodec<AllBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockPredicate.CODEC.listOf().fieldOf("block_predicates").forGetter(AllBlockPredicate::blockPredicates)).apply(i, AllBlockPredicate::new));

  @Override
  public @NotNull String asString() {
    return "all(" + String.join(", ", Collections2.transform(blockPredicates, ExpressionConvertible::asString)) + ")";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return blockPredicates.stream().allMatch(blockPredicate -> blockPredicate.test(cachedBlockPosition));
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final ImmutableList.Builder<TestResult> results = new ImmutableList.Builder<>();
    int successes = 0;
    for (BlockPredicate blockPredicate : blockPredicates) {
      TestResult testResult = blockPredicate.testAndDescribe(cachedBlockPosition);
      results.add(testResult);
      if (testResult.successes())
        successes++;
    }
    final ImmutableList<TestResult> build = results.build();
    if (successes < build.size()) {
      return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.all.fail", successes, build.size()), build);
    } else {
      return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.all.pass", successes, build.size()), build);
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.ALL;
  }

  public enum Type implements BlockPredicateType<AllBlockPredicate> {
    ALL_TYPE;

    @Override
    public @NotNull MapCodec<AllBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  public record Parser(List<BlockPredicateArgument> blockPredicates) implements FunctionParamsParser<BlockPredicateArgument> {
    public Parser() {
      this(new ArrayList<>());
    }

    @Override
    public BlockPredicateArgument getParseResult(CommandRegistryAccess registryAccess, SuggestedParser parser) {
      return source -> new AllBlockPredicate(IterateUtils.transformFailableImmutableList(blockPredicates, x -> x.apply(source)));
    }

    @Override
    public void parseParameter(CommandRegistryAccess registryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      blockPredicates.add(BlockPredicateArgument.parse(registryAccess, parser, suggestionsOnly));
    }
  }
}
