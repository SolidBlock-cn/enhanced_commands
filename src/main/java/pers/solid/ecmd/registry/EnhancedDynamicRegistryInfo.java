package pers.solid.ecmd.registry;

import com.mojang.serialization.Codec;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

public record EnhancedDynamicRegistryInfo<T>(RegistryKey<Registry<T>> registryKey, Codec<T> codec) {
}
