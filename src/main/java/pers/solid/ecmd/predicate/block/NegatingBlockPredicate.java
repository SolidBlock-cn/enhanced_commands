package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.Parser;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.TestResult;

import java.util.List;

public record NegatingBlockPredicate(BlockPredicate predicate) implements BlockPredicate {
  public static final Codec<NegatingBlockPredicate> CODEC = RecordCodecBuilder.create(i -> i.ap(NegatingBlockPredicate::new, BlockPredicate.CODEC.fieldOf("predicate").forGetter(NegatingBlockPredicate::predicate)));

  @Override
  public @NotNull String asString() {
    if (predicate instanceof NegatingBlockPredicate) {
      return "!(" + predicate.asString() + ")";
    } else {
      return "!" + predicate.asString();
    }
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return !predicate.test(cachedBlockPosition);
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final TestResult testResult = predicate.testAndDescribe(cachedBlockPosition);
    if (testResult.successes()) {
      return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.negation.fail"), List.of(testResult));
    } else {
      return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.negation.pass"), List.of(testResult));
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.NEGATING;
  }

  public enum Type implements BlockPredicateType<NegatingBlockPredicate>, Parser<BlockPredicateArgument> {
    NEGATING_TYPE;

    @Override
    public @NotNull Codec<NegatingBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable BlockPredicateArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestString("!", Text.translatable("enhanced_commands.block_predicate.negation"), suggestionsBuilder));
      boolean negates = false;
      boolean suffixed = false;
      while (parser.reader.canRead() && parser.reader.peek() == '!') {
        parser.reader.skip();
        negates = !negates;
        suffixed = true;
      }
      if (!suffixed) return null;
      if (allowsSparse) parser.reader.skipWhitespace();
      final BlockPredicateArgument parse = BlockPredicateArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
      if (negates) {
        return source -> new NegatingBlockPredicate(parse.apply(source));
      } else {
        return parse;
      }
    }
  }
}
