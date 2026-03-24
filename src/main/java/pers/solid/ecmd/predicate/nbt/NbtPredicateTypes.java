package pers.solid.ecmd.predicate.nbt;

import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;

public class NbtPredicateTypes {
  private static final RegistryBridge<NbtPredicateType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, NbtPredicateType.REGISTRY);

  // 基本的 NBT 谓词
  public static final NbtPredicateType<ComparisonNbtPredicate> COMPARISON = register("comparison", ComparisonNbtPredicate.Type.COMPARISON_TYPE);
  public static final NbtPredicateType<ConstantNbtPredicate> CONSTANT = register("constant", ConstantNbtPredicate.Type.CONSTANT_TYPE);
  public static final NbtPredicateType<EqualsCompoundNbtPredicate> EQUALS_COMPOUND = register("equals_compound", EqualsCompoundNbtPredicate.Type.EQUALS_COMPOUND_TYPE);
  public static final NbtPredicateType<EqualsListNbtPredicate> EQUALS_LIST = register("equals_list", EqualsListNbtPredicate.Type.EQUALS_LIST_TYPE);
  public static final NbtPredicateType<MatchCompoundNbtPredicate> MATCH_COMPOUND = register("match_compound", MatchCompoundNbtPredicate.Type.MATCH_COMPOUND_TYPE);
  public static final NbtPredicateType<MatchListNbtPredicate> MATCH_LIST = register("match_list", MatchListNbtPredicate.Type.MATCH_LIST_TYPE);
  public static final NbtPredicateType<MatchPrimitiveNbtPredicate> MATCH_PRIMITIVE = register("match_primitive", MatchPrimitiveNbtPredicate.Type.MATCH_PRIMITIVE_TYPE);
  public static final NbtPredicateType<RangeNbtPredicate> RANGE = register("exhaustion", RangeNbtPredicate.Type.RANGE_TYPE);
  public static final NbtPredicateType<RegexNbtPredicate> REGEX = register("regex", RegexNbtPredicate.Type.REGEX_TYPE);

  private static <T extends NbtPredicateType<?>> T register(String name, T value) {
    return REGISTRY_BRIDGE.register(name, value);
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(NbtPredicateType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
    registerFunctions();
    registerFunctionNames();
  }


  private static void registerFunctions() {

  }

  private static void registerFunctionNames() {

  }
}
