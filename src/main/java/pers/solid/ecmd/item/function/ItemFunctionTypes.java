package pers.solid.ecmd.item.function;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionsParser;

public final class ItemFunctionTypes {
  private static final RegistryBridge<ItemFunctionType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, ItemFunctionType.REGISTRY);

  public static final ItemFunctionType<EmptyItemFunction> EMPTY = register("empty", EmptyItemFunction.CODEC);
  public static final ItemFunctionType<EnchantItemFunction> ENCHANT = register("enchant", EnchantItemFunction.CODEC);
  public static final ItemFunctionType<ItemComponentCombinationItemFunction> ITEM_COMPONENT_COMBINATION = register("item_component_combination", ItemComponentCombinationItemFunction.CODEC);
  public static final ItemFunctionType<ModifyComponentItemFunction<?>> MODIFY_COMPONENT = register("modify_component", ModifyComponentItemFunction.CODEC);
  public static final ItemFunctionType<OverlayItemFunction> OVERLAY = register("overlay", OverlayItemFunction.CODEC);
  public static final ItemFunctionType<PickItemFunction> PICK = register("pick", PickItemFunction.CODEC);
  public static final ItemFunctionType<RandomItemFunction> RANDOM = register("random", RandomItemFunction.CODEC);
  public static final ItemFunctionType<RemoveComponentItemFunction<?>> REMOVE_COMPONENT = register("remove_component", RemoveComponentItemFunction.CODEC);
  public static final ItemFunctionType<SetComponentItemFunction<?>> SET_COMPONENT = register("set_component", SetComponentItemFunction.CODEC);
  public static final ItemFunctionType<SimpleItemFunction> SIMPLE = register("simple", SimpleItemFunction.CODEC);
  public static final ItemFunctionType<TagItemFunction> TAG = register("tag", TagItemFunction.CODEC);

  private ItemFunctionTypes() {
  }

  private static <T extends ItemFunctionType<?>> T register(String name, T value) {
    return REGISTRY_BRIDGE.register(name, value);
  }

  private static <T extends ItemFunction> ItemFunctionType<T> register(String name, MapCodec<T> codec) {
    return register(name, new ItemFunctionType.Simple<>(codec));
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(ItemFunctionType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
    registerFunctions();
  }

  private static void registerFunctions() {
    final FunctionsParser<ItemFunction> functionsParser = ItemFunctionParsing.FUNCTIONS_PARSER;
    functionsParser.register("enchant", Component.translatable("enhanced_commands.item_function.enchant"), EnchantItemFunction.Parser::new);
    functionsParser.register("overlay", Component.translatable("enhanced_commands.function.overlay"), OverlayItemFunction.Parser::new);
    functionsParser.register("pick", Component.translatable("enhanced_commands.function.pick"), PickItemFunction.Parser::new);
  }
}
