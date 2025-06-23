package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.parse.ParseContext;
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
  public boolean test(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    return !predicate.test(cachedBlockPosition, context);
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final TestResult testResult = predicate.testAndDescribe(cachedBlockPosition, context);
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

  public enum Type implements BlockPredicateType<NegatingBlockPredicate>, Parser<BlockPredicate> {
    NEGATING_TYPE;

    @Override
    public @NotNull MapCodec<NegatingBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public BlockPredicate parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("!", Text.translatable("enhanced_commands.block_predicate.negation"), suggestionsBuilder).buildFuture());
      boolean negates = false;
      boolean suffixed = false;
      final StringReader reader = parseContext.reader();
      while (reader.canRead() && reader.peek() == '!') {
        reader.skip();
        negates = !negates;
        suffixed = true;
      }
      if (!suffixed) return null;
      if (parseContext.allowSparse()) reader.skipWhitespace();
      if (negates && reader.canRead() && reader.peek() == '*') {
        // 此时同时读取“!*”方块谓词。
        reader.skip();
        return ConstantBlockPredicate.ALWAYS_FALSE;
      }
      final BlockPredicate parse = BlockPredicate.parse(parseContext);
      if (negates) {
        return new NegatingBlockPredicate(parse);
      } else {
        return parse;
      }
    }
  }
}
