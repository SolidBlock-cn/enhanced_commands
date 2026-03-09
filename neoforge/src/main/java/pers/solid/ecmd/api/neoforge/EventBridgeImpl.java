package pers.solid.ecmd.api.neoforge;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.common.NeoForge;
import pers.solid.ecmd.api.EventBridge;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link pers.solid.ecmd.api.EventBridge} 在 NeoForge 中的实现。
 */
public interface EventBridgeImpl<T> extends EventBridge<T> {
  /**
   * <p>基于 NeoForge 的 {@link Event} 的实现。实现示例可参见 {@link EventBridgesImpl#USE_BLOCK}。<i>请注意，暂不支持 {@link IModBusEvent}。</i>
   * <p>例如，如果 {@code T} 是一个名为 {@code MyCallback} 的接口，其方法参数为 {@code A a, B b} 并返回 {@code C}，而 {@code E} 是一个 {@link Event} 的子类，名为 {@code MyEvent}，有字段 {@code A a, B b, C c}，那么实现方式可能是像这样：
   * <pre>{@code
   * interface MyCallback {
   *   C someMethod(A a, B b);
   * }
   *
   * class MyEvent extends Event {
   *   private A a;
   *   private B b;
   *   private C c;
   *   // getter 和 setter 方法省略
   * }
   *
   *
   * final EventBridgeImpl.FromEventBus<MyCallback, MyEvent> MY_BRIDGE = new EventBridgeImpl.FromEventBus<>(
   *   // clazz:
   *   MyEvent.class,
   *   // transformer:
   *   myCallback -> myEvent -> {
   *     final C result = myCallback.someMethod(myEvent.getA(), myEvent,getB());
   *     myEvent.setC(result);
   *   },
   *   // transformerBack:
   *   consumer -> (a, b) -> {
   *     final MyEvent myEvent = new MyEvent(a, b);
   *     consumer.accept(myEvent);
   *     return myEvent.getC();
   *   });
   * }</pre>
   *
   * @param clazz           NeoForge 中的具体表示事件的类。不能是 {@link IModBusEvent}。
   * @param transformer     将类型为 {@code T} 的 listener 转化为 NeoForge 所使用的 consumer，用于在注册 listener 时，转化后传入到 NeoForge 中。这个 consumer 的参数为 {@code E}（继承 {@link Event}） 对象，且不返回值。在这个 consumer 中，需要调用 listener，其中 consumer 的参数（{@code E} 对象）的各 getter 方法需要作为 T listener 的参数，如果 T listener 有返回值，通常需要调用 {@code E} 中的 setter 方法。
   * @param transformerBack 将参数为 {@link Event} 对象并执行 {@link IEventBus#post(Event)} 的 consumer 转化为类型为 T 的 invoker，在这个 invoker 中，通常需要创建一个 {@code E} 对象（例如名称为 {@code event}），然后调用 {@code consumer.accept(event)}。如果有返回值，通常需要调用 {@code E} 中的 setter 方法。
   * @param <T>             用于 {@link EventBridge} 的 listener 类，用于注册和调用，通常是一个函数式接口。
   * @param <E>             {@link Event} 的一个子类，用于 NeoForge 的内部实现。可以是 NeoForge 自带的，也可以是模组中的。
   */
  record FromEventBus<T, E extends Event>(Class<E> clazz, Function<T, Consumer<E>> transformer, Function<Consumer<E>, T> transformerBack) implements EventBridgeImpl<T> {
    public FromEventBus {
      if (IModBusEvent.class.isAssignableFrom(clazz)) {
        throw new IllegalArgumentException("IModBusEvent events are not allowed for EventBridge at present.");
      }
    }

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
