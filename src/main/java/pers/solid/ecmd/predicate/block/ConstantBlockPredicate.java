package pers.solid.ecmd.predicate.block;

import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.SuggestionUtil;

public enum ConstantBlockPredicate implements BlockPredicate {
  ALWAYS_TRUE;

  @Override
  public @NotNull String asString() {
    return "*";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return true;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    return new TestResult(true, Text.translatable("enhancedCommands.argument.block_predicate.constant.pass"));
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.CONSTANT;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {

  }

  public enum Type implements BlockPredicateType<ConstantBlockPredicate> {
    CONSTANT_TYPE;

    @Override
    public @NotNull ConstantBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound) {
      return ALWAYS_TRUE;
    }

    @Override
    public @Nullable BlockPredicate parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) {
      parser.suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("*", Text.translatable("enhancedCommands.argument.block_predicate.constant"), suggestionsBuilder));
      if (parser.reader.canRead() && parser.reader.peek() == '*') {
        parser.reader.skip();
        parser.suggestions.clear();
        return ALWAYS_TRUE;
      } else {
        return null;
      }
    }
  }
}
