package pers.solid.ecmd.api.neoforge;

import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import pers.solid.ecmd.api.EventBridge;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implementation of {@link pers.solid.ecmd.api.EventBridge}.
 */
public interface EventBridgeImpl<T> extends EventBridge<T> {

  class Simple<T> implements EventBridgeImpl<T> {
    private final Function<T[], T> invokerFactory;
    private T[] handlers;
    protected volatile T invoker;

    @SuppressWarnings("unchecked")
    public Simple(Class<? super T> type, Function<T[], T> invokerFactory) {
      this.invokerFactory = invokerFactory;
      this.handlers = (T[]) Array.newInstance(type, 0);
      update();
    }

    void update() {
      this.invoker = invokerFactory.apply(handlers);
    }

    @Override
    public T invoker() {
      return invoker;
    }

    @Override
    public void register(T listener) {
      int oldLength = handlers.length;
      handlers = Arrays.copyOf(handlers, oldLength + 1);
      handlers[oldLength] = listener;
      update();
    }
  }

  record FromEventBus<T, E extends Event>(Class<E> clazz, Function<T, Consumer<E>> transformer, Function<Consumer<E>, T> transformerBack) implements EventBridgeImpl<T> {

    @Override
    public void register(T listener) {
      NeoForge.EVENT_BUS.addListener(clazz, transformer.apply(listener));
    }

    @Override
    public T invoker() {
      return transformerBack.apply(NeoForge.EVENT_BUS::post);
    }
  }
}
