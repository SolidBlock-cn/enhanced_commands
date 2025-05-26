package pers.solid.ecmd.util.parse;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public interface FunctionLikeParser<T> extends Parser<T> {
  default char leftPar() {
    return '(';
  }

  default char rightPar() {
    return ')';
  }

  default char separator() {
    return ',';
  }

  default String leftParString() {
    return Character.toString(leftPar());
  }

  default String rightParString() {
    return Character.toString(rightPar());
  }

  default String separatorString() {
    return Character.toString(separator());
  }

  @Override
  default T parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    if (!(reader.canRead() && reader.peek() == leftPar())) {
      return null;
    }
    reader.skip();
    return parseAfterLeftParenthesis(parseContext);
  }

  /**
   * 在完成所有参数的解析后，返回结果。通常在此接口的实现过程中，解析参数时会设置字段的一些值，此方法则使用字段中的值。
   */
  T getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException;

  /**
   * 解析括号内的内容。此时 {@code parseContext} 中的 {@code allowSparse} 通常应当是 {@code true}。
   */
  void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException;

  default T parseAfterLeftParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
    parseWithinParenthesis(parseContext.withAllowSparse(true));
    final StringReader reader = parseContext.reader();
    reader.skipWhitespace();
    parseContext.addSuggestion((context, builder) -> {
      if (builder.getRemaining().isEmpty()) {
        builder.suggest(rightParString());
      }
      return builder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == rightPar()) {
      reader.skip();
      parseContext.clearSuggestion();
      return getParseResult(parseContext);
    }
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, rightPar());
  }

  /**
   * 指定函数名称，从而让对象知晓是在解析的函数名称。通常来说，在函数命令是由 {@link FunctionsParser} 解析的，解析后分配各自的 {@link FunctionsParser}。有时抛出的异常的信息中会使用到函数名称。如果不需要使用，可以不实现此方法。
   */
  default void setFunctionName(String functionName) {
  }

  /**
   * 指定函数名称前的 cursor 的位置，从而在特定情况下抛出的异常能够指出出错的函数语法的位置。
   */
  default void setCursorBeforeFunctionName(int cursorBeforeFunctionName) {
  }
}
