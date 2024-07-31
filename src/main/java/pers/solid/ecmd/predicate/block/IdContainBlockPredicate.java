package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionParamsParser;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.regex.Pattern;

public record IdContainBlockPredicate(@NotNull Pattern pattern) implements BlockPredicate {
  public static final MapCodec<IdContainBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(IdContainBlockPredicate::new, CodecUtil.PATTERN.fieldOf("pattern").forGetter(IdContainBlockPredicate::pattern)));

  @Override
  public @NotNull String asString() {
    return "idcontain(" + NbtString.escape(pattern.pattern()) + ")";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return pattern.matcher(Registries.BLOCK.getId(cachedBlockPosition.getBlockState().getBlock()).toString()).find();
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final String id = Registries.BLOCK.getId(cachedBlockPosition.getBlockState().getBlock()).toString();
    final boolean matches = pattern.matcher(id).matches();
    return TestResult.of(matches, Text.translatable("enhanced_commands.block_predicate.id_contain." + (matches ? "pass" : "fail"), Text.literal(pattern.toString()).styled(Styles.EXPECTED), Text.literal(id).styled(Styles.ACTUAL)));
  }

  @Override
  public @NotNull BlockPredicateType<IdContainBlockPredicate> getType() {
    return BlockPredicateTypes.ID_CONTAIN;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof IdContainBlockPredicate that))
      return false;

    return pattern.pattern().equals(that.pattern.pattern());
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

  public static class Parser implements FunctionParamsParser<IdContainBlockPredicate> {
    private Pattern pattern;

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 1;
    }

    @Override
    public IdContainBlockPredicate getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) { // @formatter:on
      return new IdContainBlockPredicate(pattern);
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      parser.suggestionProviders.clear();
      pattern = ParsingUtil.readRegex(parser.reader);
    }
  }
}
