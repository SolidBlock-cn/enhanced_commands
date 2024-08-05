package pers.solid.ecmd.predicate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

public enum ConstantBlockPredicate implements BlockPredicate {
  ALWAYS_TRUE;

  public static final MapCodec<ConstantBlockPredicate> CODEC = MapCodec.unit(ConstantBlockPredicate.ALWAYS_TRUE);

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
    return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.constant.pass"));
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.CONSTANT;
  }

  public enum Type implements BlockPredicateType<ConstantBlockPredicate>, Parser<BlockPredicateArgument> {
    CONSTANT_TYPE;

    @Override
    public @NotNull MapCodec<ConstantBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable BlockPredicate parse(CommandRegistryAccess registryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestString("*", Text.translatable("enhanced_commands.block_predicate.constant"), suggestionsBuilder));
      if (parser.reader.canRead() && parser.reader.peek() == '*') {
        parser.reader.skip();
        parser.suggestionProviders.clear();
        return ALWAYS_TRUE;
      } else {
        return null;
      }
    }
  }
}
