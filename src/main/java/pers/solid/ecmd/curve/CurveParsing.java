package pers.solid.ecmd.curve;

import com.google.common.collect.Lists;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.Parser;

import java.util.List;

public class CurveParsing {
  public static final FunctionsParser<CurveProvider<?>> FUNCTIONS_PARSER = FunctionsParser.create();
  public static final List<Parser<? extends CurveProvider<?>>> PARSERS = Lists.newArrayList(FUNCTIONS_PARSER);
}
