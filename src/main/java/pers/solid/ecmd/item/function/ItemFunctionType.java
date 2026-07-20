package pers.solid.ecmd.item.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.util.DefaultNamespace;

public interface ItemFunctionType<T extends ItemFunction> {
  ResourceKey<Registry<ItemFunctionType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("item_function_type"));
  Registry<ItemFunctionType<?>> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, true);
  Codec<ItemFunctionType<?>> CODEC = DefaultNamespace.ENHANCED_COMMANDS.byNameCodecForRegistry(REGISTRY, true);

  MapCodec<T> codec();

  record Simple<T extends ItemFunction>(MapCodec<T> codec) implements ItemFunctionType<T> {
  }
}
