package pers.solid.ecmd.api;

import net.minecraft.resources.ResourceLocation;

public interface EventBridge<T> {

  T invoker();

  void register(T listener);

  default void register(ResourceLocation phase, T listener) {
    register(listener);
  }
}
