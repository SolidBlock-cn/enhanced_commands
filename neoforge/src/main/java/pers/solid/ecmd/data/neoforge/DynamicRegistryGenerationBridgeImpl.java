package pers.solid.ecmd.data.neoforge;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.data.DynamicRegistryGenerationBridge;

import java.util.Optional;

public interface DynamicRegistryGenerationBridgeImpl<T> extends DynamicRegistryGenerationBridge<T> {
  default void configureBridge(BootstrapContext<T> context) {
    configureBridge(new NeoForgeContext<>(context));
  }

  record NeoForgeContext<T>(BootstrapContext<T> bootstrap) implements ContextBridge<T> {
    @Override
    public void add(ResourceKey<T> key, T value) {
      bootstrap.register(key, value);
    }

    @Override
    public <S> HolderGetter<S> lookupOrThrow(ResourceKey<? extends Registry<? extends S>> registryKey) {
      return bootstrap.lookup(registryKey);
    }

    @Override
    public <S> Optional<HolderLookup.RegistryLookup<S>> registryLookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
      return bootstrap.registryLookup(registryKey);
    }
  }
}
