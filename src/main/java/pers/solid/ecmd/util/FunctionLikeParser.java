package pers.solid.ecmd.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;

public interface FunctionLikeParser<T> {
  Dynamic2CommandExceptionType PARAMS_TOO_FEW = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhancedCommands.paramTooFew", a, b));
  Dynamic2CommandExceptionType PARAMS_TOO_MANY = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhancedCommands.paramTooMany", a, b));

  @Contract(pure = true)
  @NotNull
  String functionName();

  @Contract(pure = true)
  default int minParamsCount() {
    return 0;
  }

  @Contract(pure = true)
  default int maxParamsCount() {
    return Integer.MAX_VALUE;
  }

  @Contract(pure = true)
  Text tooltip();

  default T parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final String name = functionName();
    if (suggestionsOnly) {
      parser.suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString(name + "(", tooltip(), suggestionsBuilder));
    }
    final int cursorBeforeUnion = parser.reader.getCursor();
    final String s = parser.reader.readUnquotedString();
    if (!(s.equals(name) && parser.reader.canRead() && parser.reader.peek() == '(')) {
      parser.reader.setCursor(cursorBeforeUnion);
      return null;
    }
    parser.reader.skip();
    // after the left parentheses
    parser.reader.skipWhitespace();
    int paramsCount = 0;

    // when allows zero params, deal with empty
    if (paramsCount >= minParamsCount()) {
      parser.suggestions.add((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          suggestionsBuilder.suggest(")");
        }
      });
    }
    if (parser.reader.canRead() && parser.reader.peek() == ')') {
      if (paramsCount >= minParamsCount()) {
        // In this case, the parameters are empty
        parser.reader.skip();
        parser.suggestions.clear();
        return getParseResult(parser);
      } else {
        throw PARAMS_TOO_FEW.createWithContext(parser.reader, paramsCount, minParamsCount());
      }
    }
    while (true) {
      parser.suggestions.clear();
      parseParameter(commandRegistryAccess, parser, paramsCount, suggestionsOnly);
      paramsCount++;
      parser.reader.skipWhitespace();
      final int finalParamsCount = paramsCount;
      parser.suggestions.add((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          if (finalParamsCount < maxParamsCount()) {
            suggestionsBuilder.suggest(",");
          }
          if (finalParamsCount >= minParamsCount()) {
            suggestionsBuilder.suggest(")");
          }
        }
      });
      // end of an expression, except a comma or right parentheses
      if (!parser.reader.canRead()) {
        if (paramsCount < minParamsCount()) {
          // params not enough, suggest comma
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(parser.reader, ",");
        } else if (paramsCount < maxParamsCount()) {
          // params enough but not full, suggest both
          throw ModCommandExceptionTypes.EXPECTED_2_SYMBOLS.createWithContext(parser.reader, ",", ")");
        } else {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(parser.reader, ")");
        }
      } else if (parser.reader.peek() == ',') {
        if (paramsCount >= maxParamsCount()) {
          throw PARAMS_TOO_MANY.createWithContext(parser.reader, paramsCount + 1, maxParamsCount());
        }
        parser.reader.skip();
        parser.reader.skipWhitespace();
        parser.suggestions.clear();
      } else if (parser.reader.peek() == ')') {
        if (paramsCount < minParamsCount()) {
          throw PARAMS_TOO_FEW.createWithContext(parser.reader, paramsCount, minParamsCount());
        }
        parser.reader.skip();
        parser.suggestions.clear();
        break;
      } else {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parser.reader);
      }
    }
    return getParseResult(parser);
  }

  T getParseResult(SuggestedParser parser) throws CommandSyntaxException;

  void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException;
}
