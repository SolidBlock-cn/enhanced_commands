package pers.solid.ecmd.api.fabric;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;

@FieldsAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RegistryBridgeImpl<T> implements RegistryBridge<T> {
  public final String namespace;
  public final Registry<T> registry;

  public RegistryBridgeImpl(String namespace, Registry<T> registry) {
    this.namespace = namespace;
    this.registry = registry;
  }

  public static <T> Registry<T> buildAndRegisterSimple(ResourceKey<Registry<T>> key) {
    return FabricRegistryBuilder.createSimple(key).attribute(RegistryAttribute.SYNCED).buildAndRegister();
  }

  public static <T> RegistryBridge<T> create(String namespace, Registry<T> vanillaRegistry) {
    return new RegistryBridgeImpl<>(namespace, vanillaRegistry);
  }

  @Override
  public boolean isEmpty() {
    return registry.size() == 0;
  }

  @Override
  public <R extends T> R register(String name, R value) {
    return Registry.register(registry, EnhancedCommands.id(name), value);
  }
}
