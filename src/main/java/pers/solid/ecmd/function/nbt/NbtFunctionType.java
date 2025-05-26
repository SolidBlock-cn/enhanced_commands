package pers.solid.ecmd.function.nbt;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import pers.solid.ecmd.EnhancedCommands;

public interface NbtFunctionType<T extends NbtFunction> {
  RegistryKey<Registry<NbtFunctionType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(EnhancedCommands.id("nbt_function_type"));
  Registry<NbtFunctionType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  MapCodec<T> getCodec();
}
