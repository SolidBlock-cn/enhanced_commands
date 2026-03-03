package pers.solid.ecmd.api;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

@FieldsAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface RegistryBridge<T> {
  @ExpectPlatform
  static <T> Registry<T> buildAndRegisterSimple(ResourceKey<Registry<T>> key) {
    throw new AssertionError();
  }

  @ExpectPlatform
  static <T> RegistryBridge<T> create(String namespace, Registry<T> vanillaRegistry) {
    throw new AssertionError();
  }

  boolean isEmpty();

  <R extends T> R register(String name, R value);

  Registry<?> registry();

  ResourceKey<? extends Registry<T>> key();
}
