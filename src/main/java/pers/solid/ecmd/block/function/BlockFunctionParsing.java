package pers.solid.ecmd.block.function;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BlockFunctionParsing {
  /**
   * 所有方块函数的函数式解析器。键为方块函数的名称，值为对应名称的方块函数解析器的 supplier。
   */
  public static final Map<String, Supplier<FunctionContentParser<? extends BlockFunction>>> FUNCTIONS = new LinkedHashMap<>();

  /**
   * 所有方块函数的函数语法的名称，将用于命令建议中的提示信息。
   */
  public static final Map<String, Component> FUNCTION_NAMES = new HashMap<>();

  /**
   * 解析方块函数中的函数语法、
   */
  public static final Parser<BlockFunction> FUNCTIONS_PARSER = new FunctionsParser<>(FUNCTIONS, FUNCTION_NAMES);
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
