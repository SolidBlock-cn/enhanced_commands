package pers.solid.ecmd.function.nbt;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;

public interface NbtFunctionType<T extends NbtFunction> {

  ResourceKey<Registry<NbtFunctionType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("nbt_function_type"));
  Registry<NbtFunctionType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  MapCodec<T> getCodec();
}
