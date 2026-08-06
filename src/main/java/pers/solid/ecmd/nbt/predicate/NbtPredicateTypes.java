package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public class NbtPredicateTypes {
  private static final RegistryBridge<NbtPredicateType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, NbtPredicateType.REGISTRY);

  // 基本的 NBT 谓词
  public static final NbtPredicateType<AllNbtPredicate> ALL = register("all", AllNbtPredicate.CODEC);
  public static final NbtPredicateType<AnyNbtPredicate> ANY = register("any", AnyNbtPredicate.CODEC);
  public static final NbtPredicateType<ComparisonNbtPredicate> COMPARISON = register("comparison", ComparisonNbtPredicate.CODEC);
  public static final NbtPredicateType<ConstantNbtPredicate> CONSTANT = register("constant", ConstantNbtPredicate.CODEC);
  public static final NbtPredicateType<EqualsCompoundNbtPredicate> EQUALS_COMPOUND = register("equals_compound", EqualsCompoundNbtPredicate.CODEC);
  public static final NbtPredicateType<EqualsListNbtPredicate> EQUALS_LIST = register("equals_list", EqualsListNbtPredicate.CODEC);
  public static final NbtPredicateType<MatchCompoundNbtPredicate> MATCH_COMPOUND = register("match_compound", MatchCompoundNbtPredicate.CODEC);
  public static final NbtPredicateType<MatchListNbtPredicate> MATCH_LIST = register("match_list", MatchListNbtPredicate.CODEC);
  public static final NbtPredicateType<MatchPrimitiveNbtPredicate> MATCH_PRIMITIVE = register("match_primitive", MatchPrimitiveNbtPredicate.CODEC);
  public static final NbtPredicateType<NegatingNbtPredicate> NEGATING = register("negating", NegatingNbtPredicate.CODEC);
  public static final NbtPredicateType<RangeNbtPredicate> RANGE = register("exhaustion", RangeNbtPredicate.CODEC);
  public static final NbtPredicateType<RegexNbtPredicate> REGEX = register("regex", RegexNbtPredicate.CODEC);

  // 特殊的 NBT 谓词
  public static final NbtPredicateType<ReferenceNbtPredicate> REFERENCE = register("reference", ReferenceNbtPredicate.CODEC);

  private static <T extends NbtPredicate> NbtPredicateType<T> register(String name, MapCodec<T> codec) {
    return register(name, new NbtPredicateType.Simple<>(codec));
  }

  private static <T extends NbtPredicateType<?>> T register(String name, T value) {
    return REGISTRY_BRIDGE.register(name, value);
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(NbtPredicateType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
    registerFunctions();
  }


  private static void registerFunctions() {
    final FunctionsParser<NbtPredicate> functionsParser = NbtPredicateParsing.FUNCTIONS_PARSER;
    functionsParser.register("all", Component.translatable("enhanced_commands.predicate.all"), AllNbtPredicate.Parser::new);
    functionsParser.register("any", Component.translatable("enhanced_commands.predicate.any"), AnyNbtPredicate.Parser::new);
    functionsParser.register("reference", Component.translatable("enhanced_commands.nbt_predicate.reference"), () -> new ReferenceEntry.ReferenceFunctionGrammarParser<>(ReferenceNbtPredicate.PREFIXED_ID_PARSER));
    functionsParser.register("regex", Component.translatable("enhanced_commands.nbt_predicate.regex"), RegexNbtPredicate.Parser::new);
  }
}
