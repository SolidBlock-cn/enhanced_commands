package pers.solid.ecmd.function.item;

import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;

public final class ItemFunctionTypes {
  private static final RegistryBridge<ItemFunctionType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, ItemFunctionType.REGISTRY);

  public static final EmptyItemFunction EMPTY = register("empty", EmptyItemFunction.INSTANCE);

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
