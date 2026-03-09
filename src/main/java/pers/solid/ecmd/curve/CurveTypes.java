package pers.solid.ecmd.curve;

import com.google.common.base.Supplier;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionLikeParser;

import java.util.Map;

public final class CurveTypes {
  private static final RegistryBridge<CurveType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, CurveType.REGISTRY);


  public static final StraightCurve.Type STRAIGHT = register("straight", StraightCurve.Type.INSTANCE);
  public static final CircleCurve.Type CIRCLE = register("circle", CircleCurve.Type.INSTANCE);

  private CurveTypes() {
  }

  public static <T extends CurveType<?>> T register(String name, T curveType) {
    return REGISTRY_BRIDGE.register(name, curveType);
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(CurveType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
    registerFunctions(CurveParsing.FUNCTIONS);
    registerFunctionNames(CurveParsing.FUNCTION_NAMES);
  }


  private static void registerFunctions(Map<String, Supplier<FunctionLikeParser<? extends CurveProvider<?>>>> map) {
    map.put("straight", StraightCurve.Parser::new);
    map.put("circle", CircleCurve.Parser::new);
  }


  private static void registerFunctionNames(Map<String, Component> map) {
  }
}
