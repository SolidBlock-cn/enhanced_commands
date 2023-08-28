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
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.TextUtil;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public record IdMatchBlockPredicate(Pattern pattern) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    return "idmatch(" + NbtString.escape(pattern.toString()) + ")";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return pattern.matcher(Registries.BLOCK.getId(cachedBlockPosition.getBlockState().getBlock()).toString()).matches();
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final String id = Registries.BLOCK.getId(cachedBlockPosition.getBlockState().getBlock()).toString();
    final boolean matches = pattern.matcher(id).matches();
    return new TestResult(matches, Text.translatable("enhancedCommands.argument.block_predicate.id_match." + (matches ? "pass" : "fail"), Text.literal(pattern.toString()).styled(TextUtil.STYLE_FOR_EXPECTED), Text.literal(id).styled(TextUtil.STYLE_FOR_ACTUAL)).formatted(matches ? Formatting.GREEN : Formatting.RED));
  }

  @Override
  public @NotNull BlockPredicateType<IdMatchBlockPredicate> getType() {
    return BlockPredicateTypes.ID_MATCH;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("pattern", pattern.toString());
  }

  public enum Type implements BlockPredicateType<IdMatchBlockPredicate> {
    ID_MATCH_TYPE;

    @Override
    public @NotNull IdMatchBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound) {
      return new IdMatchBlockPredicate(Pattern.compile(nbtCompound.getString("pattern")));
    }

    @Override
    public @Nullable IdMatchBlockPredicate parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new FunctionLikeParser<IdMatchBlockPredicate>() {// @formatter:off
        Pattern pattern;
        @Override public int minParamsCount() {return 1;}
        @Override public int maxParamsCount() {return 1;}
        @Override public @NotNull String functionName() {return "idmatch";}
        @Override public Text tooltip() {return Text.translatable("enhancedCommands.argument.block_predicate.id_match");}
        @Override
        public IdMatchBlockPredicate getParseResult(SuggestedParser parser) { // @formatter:on
          return new IdMatchBlockPredicate(pattern);
        }

        @Override
        public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
          final StringReader reader = parser.reader;
          final int cursorAtRegexBegin = reader.getCursor() + (reader.canRead() && StringReader.isQuotedStringStart(reader.peek()) ? 1 : 0);
          try {
            parser.suggestions.clear();
            pattern = Pattern.compile(reader.readString());
          } catch (PatternSyntaxException e) {
            reader.setCursor(cursorAtRegexBegin);
            throw ModCommandExceptionTypes.INVALID_REGEX.createWithContext(reader, e.getMessage());
          }
        }
      }.parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }
}
