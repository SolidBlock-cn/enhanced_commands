package pers.solid.ecmd.parse;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public interface SequentialParamListParser {
  default void parseSequentialParameters(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    reader.skipWhitespace();

    int paramsCount = 0;

    while (true) {
      parseContext.clearSuggestion();
      parsePositionalParameter(parseContext, paramsCount);
      paramsCount++;
      reader.skipWhitespace();
      // end of an expression, except a comma or right parentheses
      if (!reader.canRead()) {
        return;
      } else if (reader.peek() == separatorChar()) {
        reader.skip();
        reader.skipWhitespace();
        parseContext.clearSuggestion();
      } else {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
      }
    }
  }

  /**
   * 解析特定位置的参数。实现时需要覆盖此方法以实现对具体各参数的解析。
   *
   * @param paramIndex 参数的位置。例如，解析第一个参数时，{@code paramIndex} 为 0。
   */
  void parsePositionalParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException;

  char separatorChar();
}
