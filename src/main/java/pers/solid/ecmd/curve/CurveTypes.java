package pers.solid.ecmd.curve;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionsParser;

public final class CurveTypes {
  private static final RegistryBridge<CurveType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, CurveType.REGISTRY);


  public static final CurveType<StraightCurve> STRAIGHT = register("straight", StraightCurve.CODEC, StraightCurveProvider.CODEC);
  public static final CurveType<CircleCurve> CIRCLE = register("circle", CircleCurve.CODEC, CircleCurveProvider.CODEC);

  private CurveTypes() {
  }

  public static <T extends CurveType<?>> T register(String name, T curveType) {
    return REGISTRY_BRIDGE.register(name, curveType);
  }

  public static <T extends Curve> CurveType<T> register(String name, MapCodec<T> codec, MapCodec<? extends CurveProvider<? extends T>> providerCodec) {
    return register(name, new CurveType.Simple<>(codec, providerCodec));
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(CurveType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
    registerFunctions();
  }


  private static void registerFunctions() {
    final FunctionsParser<CurveProvider<?>> functionsParser = CurveParsing.FUNCTIONS_PARSER;
    functionsParser.register("straight", (Component) null, StraightCurve.Parser::new);
    functionsParser.register("circle", (Component) null, CircleCurve.Parser::new);
  }
}
