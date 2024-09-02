package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.List;

public record NegatingBlockPredicate(BlockPredicate predicate) implements BlockPredicate {
  public static final MapCodec<NegatingBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(NegatingBlockPredicate::new, BlockPredicate.CODEC.fieldOf("predicate").forGetter(NegatingBlockPredicate::predicate)));

  @Override
  public @NotNull String asString() {
    if (predicate instanceof NegatingBlockPredicate || predicate instanceof ConstantBlockPredicate) {
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
  public @NotNull Type getType() {
    return BlockPredicateTypes.NEGATING;
  }

  public enum Type implements BlockPredicateType<NegatingBlockPredicate>, Parser<BlockPredicateArgument> {
    NEGATING_TYPE;

    @Override
    public @NotNull MapCodec<NegatingBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable BlockPredicateArgument parse(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      parser.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("!", Text.translatable("enhanced_commands.block_predicate.negation"), suggestionsBuilder).buildFuture());
      boolean negates = false;
      boolean suffixed = false;
      while (parser.reader.canRead() && parser.reader.peek() == '!') {
        parser.reader.skip();
        negates = !negates;
        suffixed = true;
      }
      if (!suffixed) return null;
      if (allowsSparse) parser.reader.skipWhitespace();
      if (negates && parser.reader.canRead() && parser.reader.peek() == '*') {
        // 此时同时读取“!*”方块谓词。
        parser.reader.skip();
        return ConstantBlockPredicate.ALWAYS_FALSE;
      }
      final BlockPredicateArgument parse = BlockPredicateArgument.parse(registryAccess, parser, suggestionsOnly);
      if (negates) {
        return source -> new NegatingBlockPredicate(parse.apply(source));
      } else {
        return parse;
      }
    }
  }
}
