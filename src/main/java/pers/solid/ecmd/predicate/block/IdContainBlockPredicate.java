package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.regex.Pattern;

public record IdContainBlockPredicate(@NotNull Pattern pattern) implements BlockPredicate {
  public static final MapCodec<IdContainBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(IdContainBlockPredicate::new, CodecUtil.PATTERN.fieldOf("pattern").forGetter(IdContainBlockPredicate::pattern)));

  @Override
  public @NotNull String asString() {
    return "idcontain(" + StringTag.quoteAndEscape(pattern.pattern()) + ")";
  }

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    return pattern.matcher(BuiltInRegistries.BLOCK.getKey(blockInWorld.getState().getBlock()).toString()).find();
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final String id = BuiltInRegistries.BLOCK.getKey(blockInWorld.getState().getBlock()).toString();
    final boolean matches = pattern.matcher(id).find();
    return TestResult.of(matches, Component.translatable("enhanced_commands.block_predicate.id_contain." + (matches ? "pass" : "fail"), Component.literal(pattern.toString()).withStyle(Styles.EXPECTED), Component.literal(id).withStyle(Styles.ACTUAL)));
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.ID_CONTAIN;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof IdContainBlockPredicate idContainBlockPredicate))
      return false;

    return pattern.pattern().equals(idContainBlockPredicate.pattern.pattern());
  }

  @Override
  public int hashCode() {
    return pattern.pattern().hashCode();
  }

  public enum Type implements BlockPredicateType<IdContainBlockPredicate> {
    ID_CONTAIN_TYPE;

    @Override
    public @NotNull MapCodec<IdContainBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionLikeParser.SequentialParams<IdContainBlockPredicate> {
    private Pattern pattern;

    @Override
    public int minSequentialParamsCount() {
      return 1;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 1;
    }

    @Override
    public IdContainBlockPredicate getParseResult(ParseContext<?> parseContext) { // @formatter:on
      return new IdContainBlockPredicate(pattern);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      parseContext.clearSuggestion();
      pattern = ParsingUtil.readRegex(parseContext.reader());
    }
  }
}
