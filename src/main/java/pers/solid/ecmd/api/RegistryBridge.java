package pers.solid.ecmd.api;

import com.mojang.serialization.Codec;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.registry.EnhancedDynamicRegistryInfo;

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

  @ExpectPlatform
  static <T> void registerDynamicRegistry(@NotNull ResourceKey<Registry<T>> resourceKey, @NotNull Codec<T> codec, boolean sync, @NotNull InitializeContext context) {
    throw new AssertionError();
  }

  static <T> void registerDynamicRegistry(@NotNull EnhancedDynamicRegistryInfo<T> info, boolean sync, @NotNull InitializeContext context) {
    registerDynamicRegistry(info.registryKey(), info.codec(), sync, context);
  }

  boolean isEmpty();

  <R extends T> R register(String name, R value);

  Registry<?> registry();

  ResourceKey<? extends Registry<T>> key();
}
