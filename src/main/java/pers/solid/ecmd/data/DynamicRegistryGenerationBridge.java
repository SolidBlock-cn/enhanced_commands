package pers.solid.ecmd.data;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import pers.solid.ecmd.util.pack.RegistryHelper;

import java.util.Optional;

public interface DynamicRegistryGenerationBridge<T> {
  @SuppressWarnings("deprecation")
  static <T> HolderSet<T> emptyNamedSet(TagKey<T> tagKey) {
    return HolderSet.emptyNamed(RegistryHelper.safeHolderOwner(), tagKey);
  }

  void configureBridge(ContextBridge<T> context);

  interface ContextBridge<T> {
    void add(ResourceKey<T> key, T value);

    <S> HolderGetter<S> lookupOrThrow(ResourceKey<? extends Registry<? extends S>> registryKey);

    <S> Optional<? extends HolderLookup.RegistryLookup<S>> registryLookup(ResourceKey<? extends Registry<? extends S>> registryKey);
  }
}
