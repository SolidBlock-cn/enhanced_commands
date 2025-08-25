package pers.solid.ecmd.registry;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

public class EnhancedReloadableRegistries {
  private static final List<EnhancedDynamicRegistryInfo<?>> REGISTRY = new ArrayList<>();

  public static Stream<CompletableFuture<MutableRegistry<?>>> getEnhancedMutableRegistries(RegistryOps<JsonElement> ops, ResourceManager resourceManager, Executor prepareExecutor) {
    return REGISTRY.stream().map(info -> enhancedPrepare(info, ops, resourceManager, prepareExecutor));
  }

  public static <T> CompletableFuture<MutableRegistry<?>> enhancedPrepare(EnhancedDynamicRegistryInfo<T> info, RegistryOps<JsonElement> ops, ResourceManager resourceManager, Executor prepareExecutor) {
    final RegistryKey<Registry<T>> registryKey = info.registryKey();
    final Codec<T> codec = info.codec();
    return CompletableFuture.supplyAsync(() -> {
      MutableRegistry<T> mutableRegistry = new SimpleRegistry<>(registryKey, Lifecycle.experimental());
      Map<Identifier, T> map = new HashMap<>();
      JsonDataLoader.load(resourceManager, registryKey, ops, codec, map);
      map.forEach((id, value) -> mutableRegistry.add(RegistryKey.of(registryKey, id), value, new RegistryEntryInfo(Optional.empty(), Lifecycle.experimental())));
      return mutableRegistry;
    }, prepareExecutor);
  }

  public static <T> void register(RegistryKey<Registry<T>> registryKey, Codec<T> codec) {
    REGISTRY.add(new EnhancedDynamicRegistryInfo<>(registryKey, codec));
  }
}
