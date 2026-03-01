package pers.solid.ecmd.api.neoforge;

import com.google.common.base.Suppliers;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;
import pers.solid.ecmd.api.RegistryBridge;

/**
 * Implementation of {@link RegistryBridge}.
 */
@FieldsAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RegistryBridgeImpl<T> implements RegistryBridge<T> {
  public final DeferredRegister<T> deferredRegister;

  public RegistryBridgeImpl(String namespace, Registry<T> registry) {
    this.deferredRegister = DeferredRegister.create(registry, namespace);
  }

  public static <T> RegistryBridge<T> create(String namespace, Registry<T> vanillaRegistry) {
    return new RegistryBridgeImpl<>(namespace, vanillaRegistry);
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
}
