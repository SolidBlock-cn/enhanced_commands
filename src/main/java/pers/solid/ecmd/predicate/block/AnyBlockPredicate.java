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
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.FunctionParamsParser;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record AnyBlockPredicate(Collection<BlockPredicate> blockPredicates) implements BlockPredicate {
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
      return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.any.pass", successes, build.size()), build);
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.any.fail", successes, build.size()), build);
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.ANY;
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
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final BlockPredicateArgument parse = BlockPredicateArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
      blockPredicates.add(parse);
    }

    @Override
    public BlockPredicateArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) {
      return source -> new AnyBlockPredicate(IterateUtils.transformFailableImmutableList(blockPredicates, input -> input.apply(source)));
    }
  }

  public enum Type implements BlockPredicateType<AnyBlockPredicate> {
    ANY_TYPE;

    @Override
    public @NotNull AnyBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new AnyBlockPredicate(nbtCompound.getList("predicates", NbtElement.COMPOUND_TYPE).stream().map(nbtElement -> BlockPredicate.fromNbt((NbtCompound) nbtElement, world)).toList());
    }
  }
}
