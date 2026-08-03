package pers.solid.ecmd.util.pack;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * <p>表示一个对数据包内内容的引用。
 * <p>此类的存在是考虑到，在数据生成过程中，可能不存在我需要的可重载注册表的 holder，且在加载数据过程中，已知的数据并不完整。例如，一个名为 {@code enhanced_commands:grid} 的方块谓词可能存在，但是在数据加载过程中，可能并未加载，其他的谓词如果引用了它，可能读不到，且原版的数据在加载过程中（截至 1.21.1），RegistryOps 是无法读取到可重载的内容的。
 * <p>此实现有点像 {@link RegistrySetBuilder#createLazyFullPatchedRegistries(RegistryAccess, HolderLookup.Provider, Cloner.Factory, Map, HolderLookup.Provider)} 以及 {@link RegistrySetBuilder.LazyHolder}，但是原版的实现比较复杂，不太好理解，也不太好使用，所以这里自己实现了。
 */
public interface SafeReference<T> {

  ResourceKey<T> key();

  default ResourceLocation identifier() {
    return key().location();
  }

  T value(MinecraftServer server);

  @Contract(pure = true, value = "_, !null -> !null")
  default @Nullable T value(ExecutionContext executionContext, @Nullable T fallback) {
    if (executionContext.positionProvider instanceof CommandSourceStack serverSource) {
      return value(serverSource.getServer());
    } else {
      return fallback;
    }
  }

  default <E extends Throwable> T valueOrThrow(ExecutionContext executionContext, Supplier<E> exceptionSupplier) throws E {
    final @Nullable T value = value(executionContext, null);
    if (value != null) {
      return value;
    } else {
      throw exceptionSupplier.get();
    }
  }

  default T valueOrThrow(ExecutionContext executionContext) {
    final @Nullable T value = value(executionContext, null);
    if (value != null) {
      return value;
    } else {
      throw new IllegalStateException("Reference can only be applied on the server");
    }
  }

  Holder.Reference<T> holderReference(MinecraftServer server);

  static <T> Codec<Holder.Reference<T>> codec(Codec<ResourceLocation> idCodec, ResourceKey<? extends Registry<T>> registryKey) {
    return new SafeReferenceCodec<>(idCodec, registryKey);
  }

  /**
   * @see RegistryFileCodec
   */
  record SafeReferenceCodec<A>(Codec<ResourceLocation> idCodec, ResourceKey<? extends Registry<A>> registryKey) implements Codec<Holder.Reference<A>> {

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
      return DataResult.success(Pair.of(new LazyReference<>(resourceKey), idOptionalPair.get().getSecond()), Lifecycle.stable());
    }

    @Override
    public <T> DataResult<T> encode(Holder.Reference<A> input, DynamicOps<T> ops, T prefix) {
      return idCodec.encode(input.key().location(), ops, prefix);
    }
  }
}
