package pers.solid.ecmd.item.predicate;

import com.google.common.base.Supplier;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionContentParser;

import java.util.Map;

public final class ItemPredicateTypes {
  private static final RegistryBridge<ItemPredicateType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, ItemPredicateType.REGISTRY);

  public static final ItemPredicateType<AllItemPredicate> ALL_TYPE = register("all", AllItemPredicate.CODEC);
  public static final ItemPredicateType<AnyItemPredicate> ANY_TYPE = register("any", AnyItemPredicate.CODEC);
  public static final ItemPredicateType<ComponentPresenceItemPredicate<?>> COMPONENT_PRESENCE = register("component_presence", ComponentPresenceItemPredicate.CODEC);
  public static final ItemPredicateType<ComponentValueCheckItemPredicate<?>> COMPONENT_VALUE_CHECK = register("component_value_check", ComponentValueCheckItemPredicate.CODEC);
  public static final ItemPredicateType<ConstantItemPredicate> CONSTANT = register("constant", ConstantItemPredicate.CODEC);
  public static final ItemPredicateType<CountItemPredicate> COUNT = register("count", CountItemPredicate.CODEC);
  public static final ItemPredicateType<NegatingItemPredicate> NEGATING = register("negating", NegatingItemPredicate.CODEC);
  public static final ItemPredicateType<ProbabilityItemPredicate> PROBABILITY = register("probability", ProbabilityItemPredicate.CODEC);
  public static final ItemPredicateType<SimpleItemPredicate> SIMPLE = register("simple", SimpleItemPredicate.CODEC);
  public static final ItemPredicateType<SimpleCombinationItemPredicate> SIMPLE_COMBINATION = register("simple_combination", SimpleCombinationItemPredicate.CODEC);
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
