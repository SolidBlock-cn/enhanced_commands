package pers.solid.mod.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import pers.solid.mod.argument.ArgumentParser;

public interface FunctionLikeParser<T> {
  Dynamic2CommandExceptionType PARAMS_TOO_FEW = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhancedCommands.paramTooFew", a, b));
  Dynamic2CommandExceptionType PARAMS_TOO_MANY = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhancedCommands.paramTooMany", a, b));

  @Contract(pure = true)
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

  default T parse(ArgumentParser parser) throws CommandSyntaxException {
    final String name = functionName();
    if (name.startsWith(parser.reader.getRemaining().toLowerCase())) {
      parser.suggestions.add(suggestionsBuilder -> suggestionsBuilder.suggest(name + "(", tooltip()));
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
      parser.suggestions.add(suggestionsBuilder -> {
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
        return getParseResult();
      } else {
        throw PARAMS_TOO_FEW.createWithContext(parser.reader, paramsCount, minParamsCount());
      }
    }
    while (true) {
      parseParameter(parser);
      paramsCount ++;
      parser.reader.skipWhitespace();
      parser.suggestions.clear();
      final int finalParamsCount = paramsCount;
      parser.suggestions.add(suggestionsBuilder -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          if (finalParamsCount < maxParamsCount()) {
            suggestionsBuilder.suggest(",");
          }
          if (finalParamsCount >= minParamsCount()) {
            suggestionsBuilder.suggest(")");
          }
        }
      });
      // end of an expression, expect a comma or right parentheses
      if (!parser.reader.canRead()) {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(parser.reader, ")");
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
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(parser.reader, ")");
      }
    }
    return getParseResult();
  }

  T getParseResult();

  void parseParameter(ArgumentParser parser) throws CommandSyntaxException;
}
