package pers.solid.ecmd.item.function;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.block.function.IdReplaceBlockFunction;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.parse.SimpleListFunctionParser;
import pers.solid.ecmd.parse.SimpleOneArgFunctionParser;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public final class ItemFunctionTypes {
  private static final RegistryBridge<ItemFunctionType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, ItemFunctionType.REGISTRY);

  public static final ItemFunctionType<EmptyItemFunction> EMPTY = register("empty", EmptyItemFunction.CODEC);
  public static final ItemFunctionType<EnchantItemFunction> ENCHANT = register("enchant", EnchantItemFunction.CODEC);
  public static final ItemFunctionType<IdContainItemFunction> ID_CONTAIN = register("id_contain", IdContainItemFunction.CODEC);
  public static final ItemFunctionType<IdReplaceItemFunction> ID_REPLACE = register("id_replace", IdReplaceItemFunction.CODEC);
  public static final ItemFunctionType<ItemComponentCombinationItemFunction> ITEM_COMPONENT_COMBINATION = register("item_component_combination", ItemComponentCombinationItemFunction.CODEC);
  public static final ItemFunctionType<ModifyComponentItemFunction<?>> MODIFY_COMPONENT = register("modify_component", ModifyComponentItemFunction.CODEC);
  public static final ItemFunctionType<OverlayItemFunction> OVERLAY = register("overlay", OverlayItemFunction.CODEC);
  public static final ItemFunctionType<PickItemFunction> PICK = register("pick", PickItemFunction.CODEC);
  public static final ItemFunctionType<RandomItemFunction> RANDOM = register("random", RandomItemFunction.CODEC);
  public static final ItemFunctionType<ReferenceItemFunction> REFERENCE = register("reference", ReferenceItemFunction.CODEC);
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
    functionsParser.register("id-contain", Component.translatable("enhanced_commands.item_function.id_contain"), () -> new SimpleOneArgFunctionParser<>(input -> ParsingUtil.readRegex(input.reader()), IdContainItemFunction::new));
    functionsParser.register("id-replace", Component.translatable("enhanced_commands.item_function.id_replace"), () -> new IdReplaceBlockFunction.Parser<>(IdReplaceItemFunction::new));
    functionsParser.register("overlay", Component.translatable("enhanced_commands.function.overlay"), () -> new SimpleListFunctionParser<>(ItemFunction::parse, OverlayItemFunction::new));
    functionsParser.register("pick", Component.translatable("enhanced_commands.function.pick"), PickItemFunction.Parser::new);
    functionsParser.register("reference", Component.translatable("enhanced_commands.item_function.reference"), () -> new ReferenceEntry.ReferenceFunctionGrammarParser<>(ReferenceItemFunction.PREFIXED_ID_PARSER));
  }
}
