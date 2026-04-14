package pers.solid.ecmd.api;

import com.google.gson.Gson;
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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnmodifiableView;
import pers.solid.ecmd.registry.EnhancedDynamicRegistryInfo;

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
  @ApiStatus.Internal
  private static final Map<ResourceKey<? extends Registry<?>>, EnhancedDynamicRegistryInfo<?>> REGISTRY = new HashMap<>();
  private static final @UnmodifiableView Map<ResourceKey<? extends Registry<?>>, EnhancedDynamicRegistryInfo<?>> REGISTRY_VIEW = Collections.unmodifiableMap(REGISTRY);
  private static final Gson GSON = new Gson();

  public static @UnmodifiableView Map<ResourceKey<? extends Registry<?>>, EnhancedDynamicRegistryInfo<?>> getRegistry() {
    return REGISTRY_VIEW;
  }

  /**
   * 用于 mixin。
   */
  @ApiStatus.Internal
  public static Stream<CompletableFuture<WritableRegistry<?>>> getEnhancedMutableRegistries(RegistryOps<JsonElement> ops, ResourceManager resourceManager, Executor prepareExecutor) {
    return REGISTRY.values().stream().map(info -> enhancedScheduleRegistryReload(info, ops, resourceManager, prepareExecutor));
  }

  /**
   * 用于 mixin。
   */
  @ApiStatus.Internal
  public static <T> CompletableFuture<WritableRegistry<?>> enhancedScheduleRegistryReload(EnhancedDynamicRegistryInfo<T> info, RegistryOps<JsonElement> ops, ResourceManager resourceManager, Executor prepareExecutor) {
    final ResourceKey<Registry<T>> registryKey = info.registryKey();
    final Codec<T> codec = info.codec();
    return CompletableFuture.supplyAsync(() -> {
      WritableRegistry<T> mutableRegistry = new MappedRegistry<>(registryKey, Lifecycle.experimental());
      Map<ResourceLocation, JsonElement> map = new HashMap<>();
      final ResourceLocation registry = registryKey.location();
      String string = registry.getNamespace() + "/" + registry.getPath();
      SimpleJsonResourceReloadListener.scanDirectory(resourceManager, string, GSON, map);
      map.forEach((id, json) -> codec.parse(ops, json).result().ifPresent((value) -> mutableRegistry.register(ResourceKey.create(registryKey, id), value, new RegistrationInfo(Optional.empty(), Lifecycle.experimental()))));
      return mutableRegistry;
    }, prepareExecutor);
  }

  /**
   * 仅在此类中注册可重载注册表，不在 Fabric API 或 NeoForge 中注册动态注册表。
   */
  public static <T> void registerWithoutDynamicRegistry(ResourceKey<Registry<T>> registryKey, Codec<T> codec) {
    REGISTRY.put(registryKey, new EnhancedDynamicRegistryInfo<>(registryKey, codec));
  }

  /**
   * 注册可重载注册表的同时，在 Fabric API 或 NeoForge 中注册动态注册表，从而能够像常规的动态注册表那样进行数据生成。经过本模组处理，不会实际经过常规动态注册表流程加载，不会造成注册表冲突。
   */
  public static <T> void register(ResourceKey<Registry<T>> registryKey, Codec<T> codec, boolean sync, InitializeContext context) {
    registerWithoutDynamicRegistry(registryKey, codec);
    RegistryBridge.registerDynamicRegistry(registryKey, codec, sync, context);
  }
}
