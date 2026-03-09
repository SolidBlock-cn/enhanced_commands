package pers.solid.ecmd.api.neoforge;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;

/**
 * Implementation of {@link RegistryBridge}.
 */
@FieldsAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RegistryBridgeImpl<T> implements RegistryBridge<T> {
  public final DeferredRegister<T> deferredRegister;
  public final Registry<T> registry;

  public RegistryBridgeImpl(String namespace, Registry<T> registry) {
    this.deferredRegister = DeferredRegister.create(registry, namespace);
    this.registry = registry;
  }

  public static <T> RegistryBridge<T> create(String namespace, Registry<T> vanillaRegistry) {
    return new RegistryBridgeImpl<>(namespace, vanillaRegistry);
  }

  public static <T> void registerDynamicRegistry(@NotNull ResourceKey<Registry<T>> resourceKey, @NotNull Codec<T> codec, boolean sync, @NotNull InitializeContext context) {
    final IEventBus modEventBus = ((InitializeContextImpl) context).modEventBus;
    modEventBus.addListener(DataPackRegistryEvent.NewRegistry.class, event -> {
      if (sync) {
        event.dataPackRegistry(resourceKey, codec, codec);
      } else {
        event.dataPackRegistry(resourceKey, codec);
      }
    });
  }

  @Override
  public boolean isEmpty() {
    return deferredRegister.getEntries().isEmpty();
  }

  public static <T> Registry<T> buildAndRegisterSimple(ResourceKey<Registry<T>> key) {
    return new RegistryBuilder<>(key).sync(true).create();
  }

  @Override
  public <R extends T> R register(String name, R value) {
    deferredRegister.register(name, Suppliers.ofInstance(value));
    return value;
  }

  @Override
  public Registry<?> registry() {
    return registry;
  }

  @Override
  public ResourceKey<? extends Registry<T>> key() {
    return deferredRegister.getRegistryKey();
  }
}
