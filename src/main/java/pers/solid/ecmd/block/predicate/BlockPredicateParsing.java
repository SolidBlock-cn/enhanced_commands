package pers.solid.ecmd.block.predicate;

import com.google.common.collect.Lists;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.List;

public final class BlockPredicateParsing {
  public static final FunctionsParser<BlockPredicate> FUNCTIONS_PARSER = FunctionsParser.create();
  public static final Parser<BlockPredicate> PARENTHESES_PARSER = (parseContext) -> ParsingUtil.parseParentheses(() -> BlockPredicate.parse(parseContext.withAllowSparse(true)), parseContext);

  public static final List<Parser<? extends BlockPredicate>> PARSERS = Lists.newArrayList(PARENTHESES_PARSER, FUNCTIONS_PARSER);

  private BlockPredicateParsing() {
  }
}
