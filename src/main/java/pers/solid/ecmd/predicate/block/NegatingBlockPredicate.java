package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;

import java.util.List;

public record NegatingBlockPredicate(BlockPredicate blockPredicate) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    return "!" + blockPredicate.asString();
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return !blockPredicate.test(cachedBlockPosition);
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final TestResult testResult = blockPredicate.testAndDescribe(cachedBlockPosition);
    if (testResult.successes()) {
      return new TestResult(false, List.of(Text.translatable("enhancedCommands.argument.blockPredicate.negation.fail").formatted(Formatting.RED)), List.of(testResult));
    } else {
      return new TestResult(true, List.of(Text.translatable("enhancedCommands.argument.blockPredicate.negation.pass").formatted(Formatting.GREEN)), List.of(testResult));
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.NOT;
  }

  @Override
  public void writeNbt(NbtCompound nbtCompound) {
    nbtCompound.put("predicate", blockPredicate.createNbt());
  }


  public enum Type implements BlockPredicateType<NegatingBlockPredicate> {
    INSTANCE;

    @Override
    public @NotNull NegatingBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound) {
      return new NegatingBlockPredicate(BlockPredicate.fromNbt(nbtCompound.getCompound("predicate")));
    }

    @Override
    public @Nullable BlockPredicate parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      if (parser.reader.getRemaining().isEmpty())
        parser.suggestions.add((context, suggestionsBuilder) -> suggestionsBuilder.suggest("!", Text.translatable("enhancedCommands.argument.blockPredicate.negation")));
      boolean negates = false;
      boolean suffixed = false;
      while (parser.reader.canRead() && parser.reader.peek() == '!') {
        parser.reader.skip();
        negates = !negates;
        suffixed = true;
      }
      if (negates) {
        return new NegatingBlockPredicate(BlockPredicate.parse(commandRegistryAccess, parser, suggestionsOnly));
      } else if (suffixed) {
        return BlockPredicate.parse(commandRegistryAccess, parser, suggestionsOnly);
      }
      return null;
    }
  }
}
