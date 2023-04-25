package pers.solid.mod.predicate.block;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import pers.solid.mod.argument.BlockPredicateArgumentParser;
import pers.solid.mod.command.TestResult;
import pers.solid.mod.predicate.SerializablePredicate;

import java.util.Collection;
import java.util.List;

public record IntersectBlockPredicate(Collection<BlockPredicate> blockPredicates) implements BlockPredicate {
  @Override
  public String asString() {
    return "all(" + String.join(", ", Collections2.transform(blockPredicates, SerializablePredicate::asString) + ")");
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
      if (testResult.successes()) successes++;
    }
    final ImmutableList<TestResult> build = results.build();
    if (successes < build.size()) {
      return new TestResult(true, List.of(Text.translatable("blockPredicate.intersect.fail", successes, build.size()).formatted(Formatting.RED)), build);
    } else {
      return new TestResult(false, List.of(Text.translatable("blockPredicate.intersect.pass", successes, build.size()).formatted(Formatting.GREEN)), build);
    }
  }

  @Override
  public BlockPredicateType<?> getType() {
    return BlockPredicateTypes.INTERSECT;
  }

  public record Parser(ImmutableList.Builder<BlockPredicate> blockPredicates) implements FunctionLikeParser<BlockPredicate> {

    @Override
    public String functionName() {
      return "all";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("blockPredicate.intersect");
    }

    @Override
    public BlockPredicate getParseResult() {
      return new IntersectBlockPredicate(blockPredicates.build());
    }

    @Override
    public void parseParameter(BlockPredicateArgumentParser parser) throws CommandSyntaxException {
      blockPredicates.add(BlockPredicate.parse(parser));
    }
  }

  public enum Type implements BlockPredicateType<IntersectBlockPredicate> {
    INSTANCE;

    @Override
    public @Nullable BlockPredicate parse(BlockPredicateArgumentParser parser) throws CommandSyntaxException {
      return new Parser(new ImmutableList.Builder<>()).parse(parser);
    }
  }
}
