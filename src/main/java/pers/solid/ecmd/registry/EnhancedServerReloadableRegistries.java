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
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/**
 * <p>注册此模组的可重新加载的注册表，可通过 {@code /reload} 命令重新加载，类似于原版的战利品表。这些内容在数据包存储的位置与 Fabric API 和 NeoForge 中规定的一致，例如 id 为 {@code enhanced_commands:block_predicate} 的注册表的项存储于 {@code data/<命令空间>/enhanced_commands/block_function/<名称>} 中。
 * <p>这些注册表也可以通过 Fabric API 或 NeoForge 提供的 API 注册为不可重载动态注册表（类似于原版的地物配置、维度类型等），但这只是为了便于数据生成，实际在加载模组时，只要是通过此类注册为可重载注册表的注册表，均不会通过常规的不可重载动态注册表加载流程加载。
 */
public class EnhancedServerReloadableRegistries {
  private static final Map<ResourceKey<? extends Registry<?>>, EnhancedDynamicRegistryInfo<?>> REGISTRY = new HashMap<>();
  private static final @UnmodifiableView Map<ResourceKey<? extends Registry<?>>, EnhancedDynamicRegistryInfo<?>> REGISTRY_VIEW = Collections.unmodifiableMap(REGISTRY);

  public static @UnmodifiableView Map<ResourceKey<? extends Registry<?>>, EnhancedDynamicRegistryInfo<?>> getRegistry() {
    return REGISTRY_VIEW;
  }

  public static Stream<CompletableFuture<WritableRegistry<?>>> getEnhancedMutableRegistries(RegistryOps<JsonElement> ops, ResourceManager resourceManager, Executor prepareExecutor) {
    return REGISTRY.values().stream().map(info -> enhancedScheduleRegistryReload(info, ops, resourceManager, prepareExecutor));
  }

  public static <T> CompletableFuture<WritableRegistry<?>> enhancedScheduleRegistryReload(EnhancedDynamicRegistryInfo<T> info, RegistryOps<JsonElement> ops, ResourceManager resourceManager, Executor prepareExecutor) {
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
    REGISTRY.put(registryKey, new EnhancedDynamicRegistryInfo<>(registryKey, codec));
  }
}
