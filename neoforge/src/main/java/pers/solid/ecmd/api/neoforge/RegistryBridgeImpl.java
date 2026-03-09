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
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;

/**
 * {@link RegistryBridge} 在 NeoForge 中的实现。
 */
@FieldsAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RegistryBridgeImpl<T> implements RegistryBridge<T> {
  /**
   * 模组注册的内容会先存储在此对象中，并需要在模组的初始化类中调用 {@link #registerContents(InitializeContext)} 才能完成注册。
   */
  public final DeferredRegister<T> deferredRegister;
  public final Registry<T> registry;

  public RegistryBridgeImpl(String namespace, Registry<T> registry) {
    this.deferredRegister = DeferredRegister.create(registry, namespace);
    this.registry = registry;
  }

  public static <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, boolean sync) {
    return new RegistryBuilder<>(key).sync(sync).disableRegistrationCheck().create();
  }

  public static <T> RegistryBridge<T> create(String namespace, Registry<T> registry) {
    return new RegistryBridgeImpl<>(namespace, registry);
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

  public static void registerToRootRegistry(Registry<?> registry, InitializeContext context) {
    if (context instanceof InitializeContextImpl impl) {
      impl.modEventBus.addListener(NewRegistryEvent.class, event -> event.register(registry));
    }
  }

  @Override
  public boolean isEmpty() {
    return deferredRegister.getEntries().isEmpty();
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

  @Override
  public void registerContents(InitializeContext context) {
    if (!(context instanceof InitializeContextImpl impl)) {
      throw new IllegalStateException(context + " not instance of neoforge.InitializeContextImpl!");
    }
    deferredRegister.register(impl.modEventBus);
  }
}
