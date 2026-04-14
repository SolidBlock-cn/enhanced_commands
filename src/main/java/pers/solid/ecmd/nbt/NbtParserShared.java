package pers.solid.ecmd.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.concurrent.CompletableFuture;

@ApiStatus.Internal
public final class NbtParserShared {
  public static final Component START_OF_COMPOUND = Component.translatable("enhanced_commands.nbt_predicate.tooltip.start_of_compound");
  public static final Component END_OF_COMPOUND = Component.translatable("enhanced_commands.nbt_predicate.tooltip.end_of_compound");
  public static final Component START_OF_LIST = Component.translatable("enhanced_commands.nbt_predicate.tooltip.start_of_list");
  public static final Component END_OF_LIST = Component.translatable("enhanced_commands.nbt_predicate.tooltip.end_of_list");
  public static final Component SEPARATE = Component.translatable("enhanced_commands.nbt_predicate.tooltip.separate");

  private NbtParserShared() {
  }

  public static boolean parseColonOrEqual(boolean mustExpectSign, boolean equalsForDefault, StringReader reader, int cursorBeforeSign, SimpleCommandExceptionType signExpected) throws CommandSyntaxException {
    boolean isUsingEqual = equalsForDefault;
    if (!reader.canRead()) {
      reader.setCursor(cursorBeforeSign);
      throw signExpected.createWithContext(reader);
    }
    if (reader.peek() == ':') {
      isUsingEqual = false;
      reader.skip();
      reader.skipWhitespace();
    } else if (reader.peek() == '=') {
      isUsingEqual = true;
      reader.skip();
      reader.skipWhitespace();
    } else if (mustExpectSign) {
      reader.setCursor(cursorBeforeSign);
      throw signExpected.createWithContext(reader);
    }
    return isUsingEqual;
  }

  public static CompletableFuture<Suggestions> suggestCompoundStart(SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("{", START_OF_COMPOUND, suggestionsBuilder).buildFuture();
  }

  public static CompletableFuture<Suggestions> suggestCompoundSeparate(SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString(",", SEPARATE, suggestionsBuilder).buildFuture();
  }

  public static CompletableFuture<Suggestions> suggestCompoundEnd(SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("}", END_OF_COMPOUND, suggestionsBuilder).buildFuture();
  }


  public static CompletableFuture<Suggestions> suggestListSeparate(SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString(",", SEPARATE, suggestionsBuilder).buildFuture();
  }

  public static CompletableFuture<Suggestions> suggestListEnd(SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("]", END_OF_LIST, suggestionsBuilder).buildFuture();
  }

  public static <S> void handleCompoundStart(ParseContext<S> parseContext, StringReader reader) throws CommandSyntaxException {
    parseContext.setSuggestion((context, suggestionsBuilder) -> suggestCompoundStart(suggestionsBuilder));
    reader.expect('{');
    parseContext.clearSuggestion();
    reader.skipWhitespace();
  }

  public static <S> boolean handleCompoundSeparate(ParseContext<S> parseContext, StringReader reader) {
    parseContext.terminateSuggestionsIfNotEmpty();
    parseContext.addSuggestion((context, suggestionsBuilder) -> suggestCompoundSeparate(suggestionsBuilder));
    if (reader.canRead() && reader.peek() == ',') {
      reader.skip();
      parseContext.clearSuggestion();
      reader.skipWhitespace();
    } else {
      return true;
    }
    return false;
  }

  public static <S> void handleCompoundEnd(ParseContext<S> parseContext, StringReader reader) throws CommandSyntaxException {
    reader.skipWhitespace();
    parseContext.terminateSuggestionsIfNotEmpty();
    parseContext.addSuggestion((context, suggestionsBuilder) -> suggestCompoundEnd(suggestionsBuilder));
    reader.expect('}');
    parseContext.clearSuggestion();
  }

  public static <S> void handListStart(ParseContext<S> parseContext, StringReader reader) throws CommandSyntaxException {
    reader.expect('[');
    reader.skipWhitespace();
    parseContext.addSuggestion((context, builder) -> suggestListEnd(builder));
  }

  public static <S> boolean handleListSeparate(ParseContext<S> parseContext, StringReader reader) {
    assert reader == parseContext.reader();
    parseContext.terminateSuggestionsIfNotEmpty();
    parseContext.addSuggestion((context, suggestionsBuilder) -> suggestListSeparate(suggestionsBuilder));
    if (reader.canRead() && reader.peek() == ',') {
      reader.skip();
      parseContext.clearSuggestion();
      reader.skipWhitespace();
    } else {
      return true;
    }
    return false;
  }

  public static <S> void handleListEnd(ParseContext<S> parseContext, StringReader reader) throws CommandSyntaxException {
    reader.skipWhitespace();
    parseContext.addSuggestion((context, suggestionsBuilder) -> suggestListEnd(suggestionsBuilder));
    reader.expect(']');
    parseContext.clearSuggestion();
  }
}
