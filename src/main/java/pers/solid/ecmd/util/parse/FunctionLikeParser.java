package pers.solid.ecmd.util.parse;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import pers.solid.ecmd.argument.SuggestedParser;

public interface FunctionLikeParser<T> extends Parser<T> {
  default char leftOpen() {
    return '(';
  }

  default char rightOpen() {
    return ')';
  }

  default char separator() {
    return ',';
  }

  default String leftOpenString() {
    return Character.toString(leftOpen());
  }

  default String rightOpenString() {
    return Character.toString(rightOpen());
  }

  default String separatorString() {
    return Character.toString(separator());
  }

  @Override
  default T parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowSparse) throws CommandSyntaxException {
    if (!(parser.reader.canRead() && parser.reader.peek() == leftOpen())) {
      return null;
    }
    parseAfterLeftParenthesis(commandRegistryAccess, parser, suggestionsOnly);
    if (parser.reader.canRead() && parser.reader.peek() == rightOpen()) {
      return getParseResult(commandRegistryAccess, parser);
    }
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(parser.reader, rightOpen());
  }

  /**
   * 在完成所有参数的解析后，返回结果。通常在此接口的实现过程中，解析参数时会设置字段的一些值，此方法则使用字段中的值。
   */
  T getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) throws CommandSyntaxException;

  void parseWithinParenthesis(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException;

  default T parseAfterLeftParenthesis(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    parseWithinParenthesis(commandRegistryAccess, parser, suggestionsOnly);
    parser.suggestionProviders.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest(rightOpenString());
      }
    });
    if (parser.reader.canRead() && parser.reader.peek() == rightOpen()) {
      parser.reader.skip();
      parser.suggestionProviders.clear();
      return getParseResult(commandRegistryAccess, parser);
    }
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(parser.reader, rightOpen());
  }

  /**
   * 指定函数名称，从而让对象知晓是在解析的函数名称。通霄来说，在函数命令是由 {@link FunctionsParser} 解析的，解析后分配各自的 {@link FunctionsParser}。有时抛出的异常的信息中会使用到函数名称。如果不需要使用，可以不实现此方法。
   */
  default void setFunctionName(String functionName) {
  }

  /**
   * 指定函数名称前的 cursor 的位置，从而在特定情况下抛出的异常能够指出出错的函数语法的位置。
   */
  default void setCursorBeforeFunctionName(int cursorBeforeFunctionName) {
  }
}
