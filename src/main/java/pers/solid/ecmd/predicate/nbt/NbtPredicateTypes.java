package pers.solid.ecmd.predicate.nbt;

import com.google.common.base.Supplier;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionLikeParser;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class NbtPredicateTypes {
  private static final RegistryBridge<NbtPredicateType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, NbtPredicateType.REGISTRY);

  public static final Map<String, Supplier<FunctionLikeParser<? extends NbtPredicate>>> FUNCTIONS = Util.make(new LinkedHashMap<>(), NbtPredicateTypes::registerPredicates);
  public static final Map<String, Component> FUNCTION_NAMES = Util.make(new HashMap<>(), NbtPredicateTypes::registerFunctionNames);

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
    context.registerRegistry(NbtPredicateType.REGISTRY);
    context.validateAndRegister(REGISTRY_BRIDGE);
  }


  private static void registerPredicates(Map<String, Supplier<FunctionLikeParser<? extends NbtPredicate>>> map) {

  }

  private static void registerFunctionNames(Map<String, Component> map) {

  }
}
