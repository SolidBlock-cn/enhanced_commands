package pers.solid.ecmd.region;

import com.google.common.collect.Lists;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.Parser;

import java.util.List;

public final class RegionParsing {
  public static final FunctionsParser<RegionProvider<?>> FUNCTIONS_PARSER = FunctionsParser.create();
  public static final List<Parser<? extends RegionProvider<?>>> PARSERS = Lists.newArrayList(SingleBlockPosRegion.BareParser.INSTANCE, FUNCTIONS_PARSER);

  private RegionParsing() {
  }
}
