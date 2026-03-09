package pers.solid.ecmd.predicate.item;

import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;

public final class ItemPredicateTypes {
  private static final RegistryBridge<ItemPredicateType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, ItemPredicateType.REGISTRY);

  public static final ConstantItemPredicate.Type CONSTANT = register("constant", ConstantItemPredicate.Type.CONSTANT_TYPE);

  private ItemPredicateTypes() {
  }

  private static <T extends ItemPredicateType<?>> T register(String name, T value) {
    return REGISTRY_BRIDGE.register(name, value);
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(ItemPredicateType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
  }
}
