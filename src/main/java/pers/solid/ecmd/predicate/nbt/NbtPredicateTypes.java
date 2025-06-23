package pers.solid.ecmd.predicate.nbt;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.parse.FunctionLikeParser;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class NbtPredicateTypes {

  public static final Map<String, Supplier<FunctionLikeParser<? extends NbtPredicate>>> FUNCTIONS = Util.make(new LinkedHashMap<>(), NbtPredicateTypes::registerPredicates);
  public static final Map<String, Text> FUNCTION_NAMES = Util.make(new HashMap<>(), NbtPredicateTypes::registerFunctionNames);

  // 基本的 NBT 谓词
  public static final NbtPredicateType<ComparisonNbtPredicate> COMPARISON = register("comparison", ComparisonNbtPredicate.Type.COMPARISON_TYPE);
  public static final NbtPredicateType<ConstantNbtPredicate> CONSTANT = register("constant", ConstantNbtPredicate.Type.CONSTANT_TYPE);
  public static final NbtPredicateType<EqualsCompoundNbtPredicate> EQUALS_COMPOUND = register("equals_compound", EqualsCompoundNbtPredicate.Type.EQUALS_COMPOUND_TYPE);
  public static final NbtPredicateType<EqualsListNbtPredicate> EQUALS_LIST = register("equals_list", EqualsListNbtPredicate.Type.EQUALS_LIST_TYPE);
  public static final NbtPredicateType<MatchCompoundNbtPredicate> MATCH_COMPOUND = register("match_compound", MatchCompoundNbtPredicate.Type.MATCH_COMPOUND_TYPE);
  public static final NbtPredicateType<MatchListNbtPredicate> MATCH_LIST = register("match_list", MatchListNbtPredicate.Type.MATCH_LIST_TYPE);
  public static final NbtPredicateType<MatchPrimitiveNbtPredicate> MATCH_PRIMITIVE = register("match_primitive", MatchPrimitiveNbtPredicate.Type.MATCH_PRIMITIVE_TYPE);
  public static final NbtPredicateType<RangeNbtPredicate> RANGE = register("range", RangeNbtPredicate.Type.RANGE_TYPE);
  public static final NbtPredicateType<RegexNbtPredicate> REGEX = register("regex", RegexNbtPredicate.Type.REGEX_TYPE);

  private static <T extends NbtPredicateType<?>> T register(String name, T value) {
    return Registry.register(NbtPredicateType.REGISTRY, EnhancedCommands.id(name), value);
  }

  public static void init() {
    Preconditions.checkState(NbtPredicateType.REGISTRY.size() != 0);
  }


  private static void registerPredicates(Map<String, Supplier<FunctionLikeParser<? extends NbtPredicate>>> map) {

  }

  private static void registerFunctionNames(Map<String, Text> map) {

  }
}
