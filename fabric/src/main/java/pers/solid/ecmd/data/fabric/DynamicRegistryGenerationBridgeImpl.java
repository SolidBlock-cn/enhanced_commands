package pers.solid.ecmd.data.fabric;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.data.DynamicRegistryGenerationBridge;

import java.util.Optional;

public interface DynamicRegistryGenerationBridgeImpl<T> extends DynamicRegistryGenerationBridge<T> {
  default void configureBridge(HolderLookup.Provider provider, FabricDynamicRegistryProvider.Entries entries) {
    configureBridge(new FabricContext<>(provider, entries));
  }

  record FabricContext<T>(HolderLookup.Provider provider, FabricDynamicRegistryProvider.Entries entries) implements DynamicRegistryGenerationBridge.ContextBridge<T> {
    @Override
    public void add(ResourceKey<T> key, T value) {
      entries.add(key, value);
    }

    @Override
    public <S> HolderGetter<S> lookupOrThrow(ResourceKey<? extends Registry<? extends S>> registryKey) {
      return provider.lookupOrThrow(registryKey);
    }

    @Override
    public <S> Optional<? extends HolderLookup.RegistryLookup<S>> registryLookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
      return provider.lookup(registryKey);
    }
  }
}
