package pers.solid.ecmd.predicate.block;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.FunctionParamsParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record IntersectBlockPredicate(Collection<BlockPredicate> blockPredicates) implements BlockPredicate {
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
      return new TestResult(true, List.of(Text.translatable("enhanced_commands.argument.block_predicate.intersect.fail", successes, build.size()).formatted(Formatting.RED)), build);
    } else {
      return new TestResult(false, List.of(Text.translatable("enhanced_commands.argument.block_predicate.intersect.pass", successes, build.size()).formatted(Formatting.GREEN)), build);
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.INTERSECT;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    final NbtList nbtList = new NbtList();
    nbtCompound.put("predicates", nbtList);
    nbtList.addAll(Collections2.transform(blockPredicates, BlockPredicate::createNbt));
  }

  public record Parser(List<BlockPredicateArgument> blockPredicates) implements FunctionParamsParser<BlockPredicateArgument> {
    public Parser() {
      this(new ArrayList<>());
    }

    @Override
    public BlockPredicateArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) {
      return source -> new IntersectBlockPredicate(ImmutableList.copyOf(Lists.transform(blockPredicates, x -> x.apply(source))));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      blockPredicates.add(BlockPredicateArgument.parse(commandRegistryAccess, parser, suggestionsOnly));
    }
  }

  public enum Type implements BlockPredicateType<IntersectBlockPredicate> {
    INTERSECT_TYPE;

    @Override
    public @NotNull IntersectBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new IntersectBlockPredicate(nbtCompound.getList("predicates", NbtElement.COMPOUND_TYPE).stream().map(nbtElement -> BlockPredicate.fromNbt((NbtCompound) nbtElement, world)).toList());
    }
  }
}
