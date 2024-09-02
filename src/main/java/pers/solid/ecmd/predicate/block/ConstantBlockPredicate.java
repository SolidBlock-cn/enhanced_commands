package pers.solid.ecmd.predicate.block;

import com.mojang.serialization.Codec;
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
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return value;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.constant.pass"));
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.CONSTANT;
  }

  public enum Type implements BlockPredicateType<ConstantBlockPredicate>, Parser<BlockPredicateArgument> {
    CONSTANT_TYPE;

    @Override
    public @NotNull MapCodec<ConstantBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable BlockPredicate parse(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly, boolean allowsSparse) {
      parser.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("*", Text.translatable("enhanced_commands.block_predicate.constant"), suggestionsBuilder).buildFuture());
      if (parser.reader.canRead() && parser.reader.peek() == '*') {
        parser.reader.skip();
        parser.clearSuggestion();
        return ALWAYS_TRUE;
      } else {
        return null;
      }
    }
  }
}
