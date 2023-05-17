package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
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
      return new TestResult(false, List.of(Text.translatable("blockPredicate.negation.fail").formatted(Formatting.RED)), List.of(testResult));
    } else {
      return new TestResult(true, List.of(Text.translatable("blockPredicate.negation.pass").formatted(Formatting.GREEN)), List.of(testResult));
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.NOT;
  }

  @Override
  public void writeNbt(NbtCompound nbtCompound) {
    nbtCompound.put("predicate", blockPredicate.asNbt());
  }


  public enum Type implements BlockPredicateType<NegatingBlockPredicate> {
    INSTANCE;

    @Override
    public @Nullable BlockPredicate parse(SuggestedParser parser) throws CommandSyntaxException {
      if (parser.reader.getRemaining().isEmpty()) parser.suggestions.add((suggestionsBuilder, context) -> suggestionsBuilder.suggest("!", Text.translatable("blockPredicate.negation")));
      boolean negates = false;
      boolean suffixed = false;
      while (parser.reader.canRead() && parser.reader.peek() == '!') {
        parser.reader.skip();
        negates = !negates;
        suffixed = true;
      }
      if (negates) {
        return new NegatingBlockPredicate(BlockPredicate.parse(parser));
      } else if (suffixed) {
        return BlockPredicate.parse(parser);
      }
      return null;
    }
  }
}
