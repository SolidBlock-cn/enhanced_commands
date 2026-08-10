package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.parse.SimpleListFunctionParser;
import pers.solid.ecmd.parse.SimpleOneArgFunctionParser;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public final class ItemPredicateTypes {
  private static final RegistryBridge<ItemPredicateType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, ItemPredicateType.REGISTRY);

  public static final ItemPredicateType<AllItemPredicate> ALL_TYPE = register("all", AllItemPredicate.CODEC);
  public static final ItemPredicateType<AnyItemPredicate> ANY_TYPE = register("any", AnyItemPredicate.CODEC);
  public static final ItemPredicateType<ComponentPresenceItemPredicate<?>> COMPONENT_PRESENCE = register("component_presence", ComponentPresenceItemPredicate.CODEC);
  public static final ItemPredicateType<ComponentValueCheckItemPredicate<?>> COMPONENT_VALUE_CHECK = register("component_value_check", ComponentValueCheckItemPredicate.CODEC);
  public static final ItemPredicateType<ConstantItemPredicate> CONSTANT = register("constant", ConstantItemPredicate.CODEC);
  public static final ItemPredicateType<CountItemPredicate> COUNT = register("count", CountItemPredicate.CODEC);
  public static final ItemPredicateType<ItemComponentCombinationItemPredicate> ITEM_COMPONENT_COMBINATION = register("item_component_combination", ItemComponentCombinationItemPredicate.CODEC);
  public static final ItemPredicateType<IdContainItemPredicate> ID_CONTAIN = register("id_contain", IdContainItemPredicate.CODEC);
  public static final ItemPredicateType<NegatingItemPredicate> NOT = register("not", NegatingItemPredicate.CODEC);
  public static final ItemPredicateType<ProbabilityItemPredicate> PROBABILITY = register("probability", ProbabilityItemPredicate.CODEC);
  public static final ItemPredicateType<ReferenceItemPredicate> REFERENCE = register("reference", ReferenceItemPredicate.CODEC);
  public static final ItemPredicateType<SimpleItemPredicate> SIMPLE = register("simple", SimpleItemPredicate.CODEC);
  public static final ItemPredicateType<TagItemPredicate> SIMPLE_TAG = register("simple_tag", TagItemPredicate.CODEC);
  public static final ItemPredicateType<UnknownItemPredicate> UNKNOWN = register("unknown", UnknownItemPredicate.CODEC);

  private ItemPredicateTypes() {
  }

  private static <T extends ItemPredicateType<?>> T register(String name, T value) {
    return REGISTRY_BRIDGE.register(name, value);
  }

  private static <T extends ItemPredicate> ItemPredicateType<T> register(String name, MapCodec<T> codec) {
    return register(name, new ItemPredicateType.Simple<>(codec));
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(ItemPredicateType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
    registerFunctions();
  }

  private static void registerFunctions() {
    final FunctionsParser<ItemPredicate> functionsParser = ItemPredicateParsing.FUNCTIONS_PARSER;
    functionsParser.register("all", Component.translatable("enhanced_commands.item_predicate.all"), () -> new SimpleListFunctionParser<>(ItemPredicate::parse, AllItemPredicate::new));
    functionsParser.register("any", Component.translatable("enhanced_commands.item_predicate.any"), () -> new SimpleListFunctionParser<>(ItemPredicate::parse, AnyItemPredicate::new));
    functionsParser.register("count", Component.translatable("enhanced_commands.item_predicate.count"), CountItemPredicate.Parser::new);
    functionsParser.register("id-contain", Component.translatable("enhanced_commands.item_predicate.id_contain"), () -> new SimpleOneArgFunctionParser<>(input -> ParsingUtil.readRegex(input.reader()), IdContainItemPredicate::new));
    functionsParser.register("not", SimpleOneArgFunctionParser.NOT_PREDICATE_DESCRIPTION, () -> new SimpleOneArgFunctionParser<>(ItemPredicate::parse, NegatingItemPredicate::new));
    functionsParser.register("probability", Component.translatable("enhanced_commands.item_predicate.probability"), ProbabilityItemPredicate.Parser::new);
    functionsParser.register("reference", Component.translatable("enhanced_commands.item_predicate.reference"), () -> new ReferenceEntry.ReferenceFunctionGrammarParser<>(ReferenceItemPredicate.PREFIXED_ID_PARSER));
  }
}
