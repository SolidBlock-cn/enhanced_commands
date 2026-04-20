package pers.solid.ecmd.enchantment.function;

import com.mojang.serialization.MapCodec;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;

public final class EnchantmentModificationTypes {
  private static final RegistryBridge<EnchantmentModificationType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, EnchantmentModificationType.REGISTRY);

  public static final EnchantmentModificationType<AddEnchantmentModification> ADD = register("add", AddEnchantmentModification.CODEC);
  public static final EnchantmentModificationType<NaturalEnchantmentModification> NATURAL = register("natural", NaturalEnchantmentModification.CODEC);
  public static final EnchantmentModificationType<RemoveEnchantmentModification> REMOVE = register("remove", RemoveEnchantmentModification.CODEC);

  private static <T extends EnchantmentModification> EnchantmentModificationType<T> register(String name, MapCodec<T> codec) {
    return REGISTRY_BRIDGE.register(name, new EnchantmentModificationType.Simple<>(codec));
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(EnchantmentModificationType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
  }
}
