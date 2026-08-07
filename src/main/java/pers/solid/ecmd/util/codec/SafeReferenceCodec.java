package pers.solid.ecmd.util.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import pers.solid.ecmd.util.pack.LazyReference;

import java.util.Optional;

/**
 * @see RegistryFileCodec
 */
public record SafeReferenceCodec<A>(Codec<ResourceLocation> idCodec, ResourceKey<? extends Registry<A>> registryKey) implements Codec<Holder.Reference<A>> {

  @Override
  public <T> DataResult<Pair<Holder.Reference<A>, T>> decode(DynamicOps<T> ops, T input) {
    final DataResult<Pair<ResourceLocation, T>> idResult = idCodec.decode(ops, input);
    final Optional<Pair<ResourceLocation, T>> idOptionalPair = idResult.result();
    final ResourceLocation id;
    if (idOptionalPair.isEmpty()) {
      return DataResult.error(() -> "Invalid ID, reason: " + idResult.error().orElse(null));
    } else {
      id = idOptionalPair.get().getFirst();
    }

    final ResourceKey<A> resourceKey = ResourceKey.create(registryKey, id);
    if (ops instanceof RegistryOps<T> registryOps) {
      final Optional<HolderGetter<A>> optionalRegistry = registryOps.getter(registryKey);
      if (optionalRegistry.isPresent()) {
        final Optional<Holder.Reference<A>> optionalHolder = optionalRegistry.get().get(resourceKey);

        return optionalHolder
            .map(ref -> DataResult.success(Pair.of(ref, idOptionalPair.get().getSecond()), Lifecycle.stable()))
            .orElseGet(() -> DataResult.error(() -> "Cannot parse reference: registry " + registryKey.location() + " exists, but the entry with ID " + id + " does not exist"));
      }
    }
    return DataResult.success(Pair.of(LazyReference.of(resourceKey), idOptionalPair.get().getSecond()), Lifecycle.stable());
  }

  @Override
  public <T> DataResult<T> encode(Holder.Reference<A> input, DynamicOps<T> ops, T prefix) {
    return idCodec.encode(input.key().location(), ops, prefix);
  }
}
