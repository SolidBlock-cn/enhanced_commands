package pers.solid.ecmd.predicate.block;

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

public final class BlockPredicateParsing {
  public static final Map<String, Supplier<FunctionContentParser<? extends BlockPredicate>>> FUNCTIONS = new LinkedHashMap<>();
  public static final Map<String, Component> FUNCTION_NAMES = new HashMap<>();
  public static final Parser<BlockPredicate> FUNCTIONS_PARSER = new FunctionsParser<>(FUNCTIONS, FUNCTION_NAMES);
  public static final Parser<BlockPredicate> PARENTHESES_PARSER = (parseContext) -> ParsingUtil.parseParentheses(() -> BlockPredicate.parse(parseContext.withAllowSparse(true)), parseContext);

  public static final List<Parser<BlockPredicate>> PARSERS = Lists.newArrayList(PARENTHESES_PARSER, FUNCTIONS_PARSER);

  private BlockPredicateParsing() {
  }
}
