package pers.solid.ecmd.enchantment.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.util.DefaultNamespace;

public interface EnchantmentModificationType<T extends EnchantmentModification> {
  ResourceKey<Registry<EnchantmentModificationType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("enchantment_modification_type"));
  Registry<EnchantmentModificationType<?>> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, false);
  Codec<EnchantmentModificationType<?>> CODEC = DefaultNamespace.ENHANCED_COMMANDS.byNameCodecForRegistry(REGISTRY);

  MapCodec<T> codec();

  record Simple<T extends EnchantmentModification>(MapCodec<T> codec) implements EnchantmentModificationType<T> {}
}
