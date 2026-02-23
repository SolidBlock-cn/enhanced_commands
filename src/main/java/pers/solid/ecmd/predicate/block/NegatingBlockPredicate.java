package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

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
  public boolean test(BlockInWorld cachedBlockPosition, ExecutionContext context) {
    return !predicate.test(cachedBlockPosition, context);
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld cachedBlockPosition, ExecutionContext context) {
    final TestResult testResult = predicate.testAndDescribe(cachedBlockPosition, context);
    if (testResult.successes()) {
      return TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.negation.fail"), List.of(testResult));
    } else {
      return TestResult.of(true, Component.translatable("enhanced_commands.block_predicate.negation.pass"), List.of(testResult));
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
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("!", Component.translatable("enhanced_commands.block_predicate.negation"), suggestionsBuilder).buildFuture());
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
