package pers.solid.ecmd.data.fabric;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.data.DynamicRegistryGenerationBridge;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DynamicRegistryGenerationBridgeImpl<T> extends FabricDynamicRegistryProvider {

  private final DynamicRegistryGenerationBridge<T> bridge;

  public DynamicRegistryGenerationBridgeImpl(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture, DynamicRegistryGenerationBridge<T> bridge) {
    super(output, registriesFuture);
    this.bridge = bridge;
  }

  @Override
  protected void configure(HolderLookup.Provider registries, Entries entries) {
    bridge.configureBridge(new FabricContext<>(registries, entries));
  }

  @Override
  public String getName() {
    return bridge.getBridgeName();
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
