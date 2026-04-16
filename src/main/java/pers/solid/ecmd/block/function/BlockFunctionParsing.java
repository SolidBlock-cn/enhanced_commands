package pers.solid.ecmd.block.function;

import com.google.common.collect.Lists;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.List;

public class BlockFunctionParsing {
  /**
   * 解析方块函数中的函数语法、
   */
  public static final FunctionsParser<BlockFunction> FUNCTIONS_PARSER = FunctionsParser.create();
  /**
   * 解析方块函数中的括号语法。
   */
  public static final Parser<BlockFunction> PARENTHESES_PARSER = (parseContext) -> ParsingUtil.parseParentheses(() -> BlockFunction.parse(parseContext.withAllowSparse(true)), parseContext);
  /**
   * 方块函数的所有解析器。注意这个列表是可变的。
   */
  public static final List<Parser<? extends BlockFunction>> PARSERS = Lists.newArrayList(PARENTHESES_PARSER, FUNCTIONS_PARSER);

  private BlockFunctionParsing() {
  }
}
