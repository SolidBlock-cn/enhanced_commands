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
import pers.solid.ecmd.predicate.SerializablePredicate;
import pers.solid.ecmd.util.FunctionLikeParser;

import java.util.Collection;
import java.util.List;

public record IntersectBlockPredicate(Collection<BlockPredicate> blockPredicates) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
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
      if (testResult.successes())
        successes++;
    }
    final ImmutableList<TestResult> build = results.build();
    if (successes < build.size()) {
      return new TestResult(true, List.of(Text.translatable("blockPredicate.intersect.fail", successes, build.size()).formatted(Formatting.RED)), build);
    } else {
      return new TestResult(false, List.of(Text.translatable("blockPredicate.intersect.pass", successes, build.size()).formatted(Formatting.GREEN)), build);
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.INTERSECT;
  }

  @Override
  public void writeNbt(NbtCompound nbtCompound) {
    final NbtList nbtList = new NbtList();
    nbtCompound.put("predicates", nbtList);
    nbtList.addAll(Collections2.transform(blockPredicates, BlockPredicate::createNbt));
  }

  public record Parser(ImmutableList.Builder<BlockPredicate> blockPredicates) implements FunctionLikeParser<BlockPredicate> {

    @Override
    public @NotNull String functionName() {
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
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      blockPredicates.add(BlockPredicate.parse(commandRegistryAccess, parser, suggestionsOnly));
    }
  }

  public enum Type implements BlockPredicateType<IntersectBlockPredicate> {
    INSTANCE;

    @Override
    public @NotNull IntersectBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound) {
      return new IntersectBlockPredicate(nbtCompound.getList("predicates", NbtElement.COMPOUND_TYPE).stream().map(nbtElement -> BlockPredicate.fromNbt((NbtCompound) nbtCompound)).toList());
    }

    @Override
    public @Nullable BlockPredicate parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser(new ImmutableList.Builder<>()).parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }
}
