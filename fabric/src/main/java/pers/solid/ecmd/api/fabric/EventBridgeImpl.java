package pers.solid.ecmd.api.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceLocation;
import pers.solid.ecmd.api.EventBridge;

import java.util.function.Function;

public record EventBridgeImpl<T, E>(Event<E> forward, Function<T, E> transformer, Function<E, T> transformerBack) implements EventBridge<T> {
  public static <T> EventBridge<T> create(final Event<T> event) {
    return new EventBridgeImpl<>(event, Function.identity(), Function.identity());
  }

  @Override
  public T invoker() {
    return transformerBack.apply(forward.invoker());
  }

  @Override
  public void register(T listener) {
    forward.register(transformer.apply(listener));
  }

  @Override
  public void register(ResourceLocation phase, T listener) {
    forward.register(phase, transformer.apply(listener));
  }
}
