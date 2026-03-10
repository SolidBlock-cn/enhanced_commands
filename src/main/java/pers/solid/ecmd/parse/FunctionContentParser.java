package pers.solid.ecmd.parse;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

public interface FunctionContentParser<T> {
  /**
   * 在完成所有参数的解析后，返回结果。通常在此接口的实现过程中，解析参数时会设置字段的一些值，此方法则使用字段中的值。
   */
  T getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException;

  /**
   * 解析括号内的内容。此时 {@code parseContext} 中的 {@code allowSparse} 通常应当是 {@code true}。
   */
  void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException;

  /**
   * 在 {@link FunctionsParser} 的解析过程中，在左括号的的左边执行。
   *
   * @implNote 实现此方法可用于收集解析到的函数名称以及名称前后的 cursor。
   */
  default void onBeforeParentheses(String functionName, int cursorBeforeFunctionName, int cursorAfterFunctionName) {
  }

  /**
   * 在 {@link FunctionsParser} 的解析过程中，在右括号的右边执行。
   *
   * @implNote 实现此方法可收集右括号右边的 cursor。
   */
  default void onAfterParentheses(int cursorAfterParentheses) {
  }


  interface SequentialParams<T> extends FunctionContentParser<T>, SequentialParamListParser {
    @Override
    default void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      parseSequentialParameters(parseContext);
    }
  }

  interface NamedParams<T> extends FunctionContentParser<T>, NamedParamListParser {
    @Override
    default void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      parseNamedParameters(parseContext);
    }
  }

  interface MixedParams<T> extends FunctionContentParser<T>, MixedParamListParser {
    @Override
    default void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      parseMixedParameters(parseContext);
    }
  }
}
