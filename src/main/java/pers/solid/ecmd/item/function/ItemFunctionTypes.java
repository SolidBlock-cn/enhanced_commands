package pers.solid.ecmd.item.function;

import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;

public final class ItemFunctionTypes {
  private static final RegistryBridge<ItemFunctionType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, ItemFunctionType.REGISTRY);

  public static final EmptyItemFunction EMPTY = register("empty", EmptyItemFunction.INSTANCE);
  public static final ItemComponentCombinationItemFunction.Type ITEM_COMPONENT_COMBINATION = register("item_component_combination", ItemComponentCombinationItemFunction.Type.ITEM_COMPONENT_COMBINATION_TYPE);
  public static final OverlayItemFunction.Type OVERLAY = register("overlay", OverlayItemFunction.Type.OVERLAY_TYPE);
  public static final PickItemFunction.Type PICK = register("pick", PickItemFunction.Type.PICK_TYPE);
  public static final RandomItemFunction.Type RANDOM = register("random", RandomItemFunction.Type.RANDOM_TYPE);
  public static final RemoveComponentItemFunction.Type REMOVE_COMPONENT = register("remove_component", RemoveComponentItemFunction.Type.REMOVE_COMPONENT_TYPE);
  public static final SetComponentItemFunction.Type SET_COMPONENT = register("set_component", SetComponentItemFunction.Type.SET_COMPONENT_TYPE);
  public static final SimpleItemFunction.Type SIMPLE = register("simple", SimpleItemFunction.Type.SIMPLE_TYPE);

  private ItemFunctionTypes() {
  }

  private static <T extends ItemFunctionType<?>> T register(String name, T value) {
    return REGISTRY_BRIDGE.register(name, value);
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(ItemFunctionType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
  }
}
