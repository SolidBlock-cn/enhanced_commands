package pers.solid.ecmd.region;

import com.google.common.collect.Lists;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.Parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RegionParsing {
  public static final Map<String, RegionType<?>> FUNCTIONS = new LinkedHashMap<>();
  public static final List<Parser<? extends RegionProvider<?>>> PARSERS = Lists.newArrayList(SingleBlockPosRegion.BareParser.INSTANCE, new FunctionsParser<>(FUNCTIONS.keySet(), s -> {
    final RegionType<?> regionType = FUNCTIONS.get(s);
    return regionType == null ? null : regionType.parser();
  }, s -> {
    final RegionType<?> regionType = FUNCTIONS.get(s);
    return regionType == null ? null : regionType.tooltip();
  }));

  private RegionParsing() {
  }
}
