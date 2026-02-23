package pers.solid.ecmd.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public record EnhancedDynamicRegistryInfo<T>(ResourceKey<Registry<T>> registryKey, Codec<T> codec) {
}
