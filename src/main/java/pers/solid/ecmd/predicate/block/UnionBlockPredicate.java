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
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.FunctionLikeParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record UnionBlockPredicate(Collection<BlockPredicate> blockPredicates) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    return "any(" + String.join(", ", Collections2.transform(blockPredicates, ExpressionConvertible::asString)) + ")";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return blockPredicates.stream().anyMatch(blockPredicate -> blockPredicate.test(cachedBlockPosition));
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
    if (successes > 0) {
      return new TestResult(true, List.of(Text.translatable("enhancedCommands.argument.block_predicate.union.pass", successes, build.size()).formatted(Formatting.GREEN)), build);
    } else {
      return new TestResult(false, List.of(Text.translatable("enhancedCommands.argument.block_predicate.union.fail", successes, build.size()).formatted(Formatting.RED)), build);
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.UNION;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    final NbtList nbtList = new NbtList();
    nbtCompound.put("predicates", nbtList);
    nbtList.addAll(Collections2.transform(blockPredicates, BlockPredicate::createNbt));
  }

  public record Parser(List<BlockPredicateArgument> blockPredicates) implements FunctionLikeParser<BlockPredicateArgument> {

    @Override
    public @NotNull String functionName() {
      return "any";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.block_predicate.union");
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final BlockPredicateArgument parse = BlockPredicateArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
      blockPredicates.add(parse);
    }

    @Override
    public BlockPredicateArgument getParseResult(SuggestedParser parser) {
      return source -> new UnionBlockPredicate(ImmutableList.copyOf(Lists.transform(blockPredicates, input -> input.apply(source))));
    }
  }

  public enum Type implements BlockPredicateType<UnionBlockPredicate> {
    UNION_TYPE;

    @Override
    public @NotNull UnionBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new UnionBlockPredicate(nbtCompound.getList("predicates", NbtElement.COMPOUND_TYPE).stream().map(nbtElement -> BlockPredicate.fromNbt((NbtCompound) nbtElement, world)).toList());
    }

    @Override
    public @Nullable BlockPredicateArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      return new Parser(new ArrayList<>()).parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }
}
