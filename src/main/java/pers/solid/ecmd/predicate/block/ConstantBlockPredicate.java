package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

public enum ConstantBlockPredicate implements BlockPredicate {
  ALWAYS_TRUE(true),
  ALWAYS_FALSE(false);
  private final boolean value;

  public static final MapCodec<ConstantBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codec.BOOL.optionalFieldOf("value", true).forGetter(ConstantBlockPredicate::value)).apply(i, ConstantBlockPredicate::of));

  ConstantBlockPredicate(boolean value) {
    this.value = value;
  }

  public boolean value() {
    return value;
  }

  public static ConstantBlockPredicate of(boolean value) {
    return value ? ALWAYS_TRUE : ALWAYS_FALSE;
  }

  @Override
  public @NotNull String asString() {
    return value ? "*" : "!*";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    return value;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    if (value) {
      return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.constant.pass"));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.constant.fail"));
    }
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.CONSTANT;
  }

  public enum Type implements BlockPredicateType<ConstantBlockPredicate>, Parser<ConstantBlockPredicate> {
    CONSTANT_TYPE;

    @Override
    public @NotNull MapCodec<ConstantBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public ConstantBlockPredicate parse(ParseContext<?> parseContext) {
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("*", Text.translatable("enhanced_commands.block_predicate.constant"), suggestionsBuilder).buildFuture());
      final StringReader reader = parseContext.reader();
      if (reader.canRead() && reader.peek() == '*') {
        reader.skip();
        parseContext.clearSuggestion();
        return ALWAYS_TRUE;
      } else {
        return null;
      }
    }
  }
}
