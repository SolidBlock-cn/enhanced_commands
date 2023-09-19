package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.FunctionLikeParser;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TextUtil;

import java.util.regex.Pattern;

public record IdContainBlockPredicate(@NotNull Pattern pattern) implements BlockPredicate {
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
    return new TestResult(matches, Text.translatable("enhancedCommands.argument.block_predicate.id_contain." + (matches ? "pass" : "fail"), Text.literal(pattern.toString()).styled(TextUtil.STYLE_FOR_EXPECTED), Text.literal(id).styled(TextUtil.STYLE_FOR_ACTUAL)).formatted(matches ? Formatting.GREEN : Formatting.RED));
  }

  @Override
  public @NotNull BlockPredicateType<IdContainBlockPredicate> getType() {
    return BlockPredicateTypes.ID_CONTAIN;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("pattern", pattern.toString());
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
    public @NotNull IdContainBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound) {
      return new IdContainBlockPredicate(Pattern.compile(nbtCompound.getString("pattern")));
    }

    @Override
    public @Nullable IdContainBlockPredicate parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new FunctionLikeParser<IdContainBlockPredicate>() {// @formatter:off
        Pattern pattern;
        @Override public int minParamsCount() {return 1;}
        @Override public int maxParamsCount() {return 1;}
        @Override public @NotNull String functionName() {return "idcontain";}
        @Override public Text tooltip() {return Text.translatable("enhancedCommands.argument.block_predicate.id_contain");}
        @Override
        public IdContainBlockPredicate getParseResult(SuggestedParser parser) { // @formatter:on
          return new IdContainBlockPredicate(pattern);
        }

        @Override
        public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
          final StringReader reader = parser.reader;
          final int cursorAtRegexBegin = reader.getCursor() + (reader.canRead() && StringReader.isQuotedStringStart(reader.peek()) ? 1 : 0);
          parser.suggestions.clear();
          pattern = StringUtil.readRegex(parser.reader);
        }
      }.parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }
}
