package pers.solid.ecmd.curve;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.Parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CurveTypes {
  private static final RegistryBridge<CurveType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, CurveType.REGISTRY);

  public static final Map<String, Supplier<FunctionLikeParser<? extends CurveProvider<?>>>> FUNCTIONS = Util.make(new LinkedHashMap<>(), CurveTypes::registerFunctions);
  public static final Map<String, Component> FUNCTION_NAMES = Util.make(new LinkedHashMap<>(), CurveTypes::registerFunctionNames);
  public static final Parser<CurveProvider<?>> FUNCTIONS_PARSER = new FunctionsParser<>(FUNCTIONS, FUNCTION_NAMES);
  public static final List<Parser<CurveProvider<?>>> PARSERS = Lists.newArrayList(FUNCTIONS_PARSER);


  public static final StraightCurve.Type STRAIGHT = register("straight", StraightCurve.Type.INSTANCE);
  public static final CircleCurve.Type CIRCLE = register("circle", CircleCurve.Type.INSTANCE);

  private CurveTypes() {
  }

  public static <T extends CurveType<?>> T register(String name, T curveType) {
    return REGISTRY_BRIDGE.register(name, curveType);
  }

  public static void init(InitializeContext context) {
    context.registerRegistry(CurveType.REGISTRY);
    context.validateAndRegister(REGISTRY_BRIDGE);
  }


  private static void registerFunctions(Map<String, Supplier<FunctionLikeParser<? extends CurveProvider<?>>>> map) {
    map.put("straight", StraightCurve.Parser::new);
    map.put("circle", CircleCurve.Parser::new);
  }


  private static void registerFunctionNames(Map<String, Component> map) {
  }
}
