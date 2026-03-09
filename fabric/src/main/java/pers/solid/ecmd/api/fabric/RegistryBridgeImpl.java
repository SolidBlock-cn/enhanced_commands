package pers.solid.ecmd.api.fabric;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;

/**
 * {@link RegistryBridge} 在 Fabric 中的实现。
 */
@FieldsAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RegistryBridgeImpl<T> implements RegistryBridge<T> {
  public final String namespace;
  public final Registry<T> registry;

  public RegistryBridgeImpl(String namespace, Registry<T> registry) {
    this.namespace = namespace;
    this.registry = registry;
  }

  /**
   * 创建一个注册表，并在 Fabric API 中立即注册至根注册表。
   *
   * @param key  {@inheritDoc}
   * @param sync {@inheritDoc}
   * @return {@inheritDoc}
   */
  public static <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, boolean sync) {
    final FabricRegistryBuilder<T, MappedRegistry<T>> builder = FabricRegistryBuilder.createSimple(key);
    if (sync) {
      builder.attribute(RegistryAttribute.SYNCED);
    }
    return builder.attribute(RegistryAttribute.SYNCED).buildAndRegister();
  }

  public static <T> RegistryBridge<T> create(String namespace, Registry<T> registry) {
    return new RegistryBridgeImpl<>(namespace, registry);
  }

  /**
   * 使用 Fabric API，将其注册为动态注册表。
   *
   * @see DynamicRegistries
   */
  public static <T> void registerDynamicRegistry(@NotNull ResourceKey<Registry<T>> resourceKey, @NotNull Codec<T> codec, boolean sync, @NotNull InitializeContext context) {
    if (sync) {
      DynamicRegistries.registerSynced(resourceKey, codec);
    } else {
      DynamicRegistries.register(resourceKey, codec);
    }
  }

  /**
   * 在 Fabric 中不执行操作，因为已经在 {@link #createRegistry(ResourceKey, boolean)} 中完成对 {@link FabricRegistryBuilder#buildAndRegister()} 的调用。
   */
  public static void registerToRootRegistry(Registry<?> registry, InitializeContext context) {
  }

  @Override
  public boolean isEmpty() {
    return registry.size() == 0;
  }

  @Override
  public <R extends T> R register(String name, R value) {
    return Registry.register(registry, EnhancedCommands.id(name), value);
  }

  @Override
  public Registry<?> registry() {
    return registry;
  }

  @Override
  public ResourceKey<? extends Registry<T>> key() {
    return registry.key();
  }

  /**
   * 在 Fabric 中不执行操作，因为已经调用了 {@link #register(String, Object)}。尽管如此，应当在模组的 Fabric 与 NeoForge 共用的模组初始化代码中调用此方法，以确保内容在 NeoForge 中也正常注册。
   */
  @Override
  public void registerContents(InitializeContext context) {
  }
}
