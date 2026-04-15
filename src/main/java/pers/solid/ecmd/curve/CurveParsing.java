package pers.solid.ecmd.curve;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.Parser;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CurveParsing {
  public static final Map<String, Supplier<FunctionContentParser<? extends CurveProvider<?>>>> FUNCTIONS = new LinkedHashMap<>();
  public static final Map<String, Component> FUNCTION_NAMES = new HashMap<>();
  public static final Parser<CurveProvider<?>> FUNCTIONS_PARSER = new FunctionsParser<>(FUNCTIONS, FUNCTION_NAMES);
  public static final List<Parser<? extends CurveProvider<?>>> PARSERS = Lists.newArrayList(FUNCTIONS_PARSER);
}
