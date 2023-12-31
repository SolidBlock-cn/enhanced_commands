package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;
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

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putBoolean("same", same);
    nbtCompound.put("predicate1", blockPredicate1.createNbt());
    nbtCompound.put("predicate2", blockPredicate2.createNbt());
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

  public enum Type implements BlockPredicateType<BiPredicateBlockPredicate> {
    BI_PREDICATE_TYPE;

    @Override
    public @NotNull BiPredicateBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      final boolean same = nbtCompound.getBoolean("same");
      final BlockPredicate predicate1 = BlockPredicate.fromNbt(nbtCompound.getCompound("predicate1"), world);
      final BlockPredicate predicate2 = BlockPredicate.fromNbt(nbtCompound.getCompound("predicate2"), world);
      return new BiPredicateBlockPredicate(predicate1, predicate2, same);
    }
  }
}
