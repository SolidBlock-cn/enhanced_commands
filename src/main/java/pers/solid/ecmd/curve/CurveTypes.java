package pers.solid.ecmd.curve;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.Parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CurveTypes {
  public static final Map<String, Supplier<FunctionLikeParser<? extends CurveArgument<?>>>> FUNCTIONS = Util.make(new LinkedHashMap<>(), CurveTypes::registerFunctions);
  public static final Map<String, Text> FUNCTION_NAMES = Util.make(new LinkedHashMap<>(), CurveTypes::registerFunctionNames);
  public static final Parser<CurveArgument<?>> FUNCTIONS_PARSER = new FunctionsParser<>(FUNCTIONS, FUNCTION_NAMES);
  public static final List<Parser<CurveArgument<?>>> PARSERS = Lists.newArrayList(FUNCTIONS_PARSER);


  public static final StraightCurve.Type STRAIGHT = register("straight", StraightCurve.Type.INSTANCE);
  public static final CircleCurve.Type CIRCLE = register("circle", CircleCurve.Type.INSTANCE);

  private CurveTypes() {
  }

  public static <T extends CurveType<?>> T register(String name, T curveType) {
    return Registry.register(CurveType.REGISTRY, EnhancedCommands.id(name), curveType);
  }

  public static void init() {
    Preconditions.checkState(CurveType.REGISTRY.size() != 0, "CurveType registry is empty");
  }


  private static void registerFunctions(Map<String, Supplier<FunctionLikeParser<? extends CurveArgument<?>>>> map) {
    map.put("straight", StraightCurve.Parser::new);
    map.put("circle", CircleCurve.Parser::new);
  }


  private static void registerFunctionNames(Map<String, Text> map) {
  }
}
