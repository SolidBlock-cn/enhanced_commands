package pers.solid.ecmd.predicate.item;

import com.google.common.base.Supplier;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionContentParser;

import java.util.Map;

public final class ItemPredicateTypes {
  private static final RegistryBridge<ItemPredicateType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, ItemPredicateType.REGISTRY);

  public static final AllItemPredicate.Type ALL_TYPE = register("all", AllItemPredicate.Type.ALL_TYPE);
  public static final AnyItemPredicate.Type ANY_TYPE = register("any", AnyItemPredicate.Type.ANY_TYPE);
  public static final ComponentPresenceItemPredicate.Type COMPONENT_PRESENCE = register("component_presence", ComponentPresenceItemPredicate.Type.COMPONENT_PRESENCE_TYPE);
  public static final ComponentValueCheckItemPredicate.Type COMPONENT_VALUE_CHECK = register("component_value_check", ComponentValueCheckItemPredicate.Type.COMPONENT_VALUE_CHECK_TYPE);
  public static final ConstantItemPredicate.Type CONSTANT = register("constant", ConstantItemPredicate.Type.CONSTANT_TYPE);
  public static final CountItemPredicate.Type COUNT = register("count", CountItemPredicate.Type.COUNT_TYPE);
  public static final NegatingItemPredicate.Type NEGATING = register("negating", NegatingItemPredicate.Type.NEGATING_TYPE);
  public static final ProbabilityItemPredicate.Type PROBABILITY = register("probability", ProbabilityItemPredicate.Type.PROBABILITY_TYPE);
  public static final SimpleItemPredicate.Type SIMPLE = register("simple", SimpleItemPredicate.Type.SIMPLE_TYPE);
  public static final SimpleCombinationItemPredicate.Type SIMPLE_COMBINATION = register("simple_combination", SimpleCombinationItemPredicate.Type.SIMPLE_COMBINATION_TYPE);
  public static final TagItemPredicate.Type SIMPLE_TAG = register("simple_tag", TagItemPredicate.Type.SIMPLE_TAG_TYPE);
  public static final UnknownItemPredicate.Type UNKNOWN = register("unknown", UnknownItemPredicate.Type.UNKNOWN_TYPE);

  private ItemPredicateTypes() {
  }

  private static <T extends ItemPredicateType<?>> T register(String name, T value) {
    return REGISTRY_BRIDGE.register(name, value);
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(ItemPredicateType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
    registerFunctions();
    registerFunctionNames();
  }

  private static void registerFunctions() {
    final Map<String, Supplier<FunctionContentParser<? extends ItemPredicate>>> map = ItemPredicateParsing.FUNCTIONS;
    map.put("all", AllItemPredicate.Parser::new);
    map.put("any", AnyItemPredicate.Parser::new);
    map.put("count", CountItemPredicate.Parser::new);
    map.put("probability", ProbabilityItemPredicate.Parser::new);
  }

  private static void registerFunctionNames() {
    final Map<String, Component> map = ItemPredicateParsing.FUNCTION_NAMES;
    map.put("all", Component.translatable("enhanced_commands.item_predicate.all"));
    map.put("any", Component.translatable("enhanced_commands.item_predicate.any"));
    map.put("count", Component.translatable("enhanced_commands.item_predicate.count"));
    map.put("probability", Component.translatable("enhanced_commands.item_predicate.probability"));
  }
}
