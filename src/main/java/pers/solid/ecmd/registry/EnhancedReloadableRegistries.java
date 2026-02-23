package pers.solid.ecmd.registry;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

public class EnhancedReloadableRegistries {
  private static final List<EnhancedDynamicRegistryInfo<?>> REGISTRY = new ArrayList<>();

  public static Stream<CompletableFuture<WritableRegistry<?>>> getEnhancedMutableRegistries(RegistryOps<JsonElement> ops, ResourceManager resourceManager, Executor prepareExecutor) {
    return REGISTRY.stream().map(info -> enhancedPrepare(info, ops, resourceManager, prepareExecutor));
  }

  public static <T> CompletableFuture<WritableRegistry<?>> enhancedPrepare(EnhancedDynamicRegistryInfo<T> info, RegistryOps<JsonElement> ops, ResourceManager resourceManager, Executor prepareExecutor) {
    final ResourceKey<Registry<T>> registryKey = info.registryKey();
    final Codec<T> codec = info.codec();
    return CompletableFuture.supplyAsync(() -> {
      WritableRegistry<T> mutableRegistry = new MappedRegistry<>(registryKey, Lifecycle.experimental());
      Map<ResourceLocation, T> map = new HashMap<>();
      SimpleJsonResourceReloadListener.scanDirectory(resourceManager, registryKey, ops, codec, map);
      map.forEach((id, value) -> mutableRegistry.register(ResourceKey.create(registryKey, id), value, new RegistrationInfo(Optional.empty(), Lifecycle.experimental())));
      return mutableRegistry;
    }, prepareExecutor);
  }

  public static <T> void register(ResourceKey<Registry<T>> registryKey, Codec<T> codec) {
    REGISTRY.add(new EnhancedDynamicRegistryInfo<>(registryKey, codec));
  }
}
