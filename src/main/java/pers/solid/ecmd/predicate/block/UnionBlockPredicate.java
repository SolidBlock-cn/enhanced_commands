package pers.solid.ecmd.predicate.block;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.StringRepresentablePredicate;
import pers.solid.ecmd.util.FunctionLikeParser;

import java.util.Collection;
import java.util.List;

public record UnionBlockPredicate(Collection<BlockPredicate> blockPredicates) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    return "any(" + String.join(", ", Collections2.transform(blockPredicates, StringRepresentablePredicate::asString)) + ")";
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
      return new TestResult(true, List.of(Text.translatable("enhancedCommands.argument.blockPredicate.union.pass", successes, build.size()).formatted(Formatting.GREEN)), build);
    } else {
      return new TestResult(false, List.of(Text.translatable("enhancedCommands.argument.blockPredicate.union.fail", successes, build.size()).formatted(Formatting.RED)), build);
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

  public record Parser(ImmutableList.Builder<BlockPredicate> blockPredicates) implements FunctionLikeParser<UnionBlockPredicate> {

    @Override
    public @NotNull String functionName() {
      return "any";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.blockPredicate.union");
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final BlockPredicate parse = BlockPredicate.parse(commandRegistryAccess, parser, suggestionsOnly);
      blockPredicates.add(parse);
    }

    @Override
    public UnionBlockPredicate getParseResult() {
      return new UnionBlockPredicate(blockPredicates.build());
    }
  }

  public enum Type implements BlockPredicateType<UnionBlockPredicate> {
    INSTANCE;

    @Override
    public @NotNull UnionBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound) {
      return new UnionBlockPredicate(nbtCompound.getList("predicates", NbtElement.COMPOUND_TYPE).stream().map(nbtElement -> BlockPredicate.fromNbt((NbtCompound) nbtElement)).toList());
    }

    @Override
    public @Nullable BlockPredicate parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser(new ImmutableList.Builder<>()).parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }
}
