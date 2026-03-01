package pers.solid.ecmd.function.nbt;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;

public interface NbtFunctionType<T extends NbtFunction> {

  ResourceKey<Registry<NbtFunctionType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("nbt_function_type"));
  Registry<NbtFunctionType<?>> REGISTRY = RegistryBridge.buildAndRegisterSimple(REGISTRY_KEY);

  MapCodec<T> getCodec();
}
