package pers.solid.ecmd.api.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.resources.ResourceLocation;
import pers.solid.ecmd.api.EventBridge;
import pers.solid.ecmd.api.EventBridges.UseBlockCallbackBridge;
import pers.solid.ecmd.api.FlipStateCallback;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link EventBridge} 在 Fabric 中的实现。
 */
public interface EventBridgeImpl<T> extends EventBridge<T> {
  /**
   * <p>基于 Fabric API 中的 {@link Event} 的事件。此类需要指定两个类型参数：{@code T} 和 {@code E}。{@code T} 用于此模组的 {@link EventBridge}，而 {@code E} 用于 Fabric API 的 {@link Event}。
   * <ul>
   * <li>如果是基于 Fabric API 已有的 Event，则 {@code E} 是 Fabric API 中已有的 listener 类（如 {@link UseBlockCallback}），而 {@code T} 不能是 Fabric API 中的（因为要在非 Fabric 环境下使用），可以是与 Fabric API 中的接口相似的接口（如 {@link UseBlockCallbackBridge UseBlockCallbackBridge}），也可以是简单的接口（如 {@link Consumer}、{@link Runnable} 等）。实现示例可见 {@link EventBridgesImpl#USE_BLOCK}。
   * <li>如果模组自己创建的事件，而非 Fabric API 中的，那么 {@code T} 和 {@code E} 可以是一个相同的模组自带的类（如 {@link FlipStateCallback}），此时建议使用 {@link #createIdentical}。实现示例可见 {@link EventBridgesImpl#FLIP_STATE}。
   * </ul>
   *
   * @param forward         Fabric API 中的 Event。
   * @param transformer     将 {@code EventBridge} 中的 listener 转化为 Fabric API 的 Event 实际使用的 listener 的 function。如果类型参数 {@code T} 和 {@code E} 相同，可使用方法引用（双冒号语法）。会在注册 listener 时使用。
   * @param transformerBack 将 Fabric API 的 Event 实际使用的 listener 转化为 {@code EventBridge} 中的 listener 的 function。如果类型参数 {@code T} 和 {@code E} 相同，可使用方法引用（双冒号语法）。会在事件调用时使用。
   * @param <T>             此 {@link EventBridge} 对外使用的 listener 的类型，通常不是 Fabric API 中的类，因为可能会在非 Fabric 环境下使用。
   * @param <E>             对应的 Fabric API 中的 Event 的 listener 的类，即 {@code Event<E> forward} 中的类型参数。
   */
  record FromFabricEvent<T, E>(Event<E> forward, Function<T, E> transformer, Function<E, T> transformerBack) implements EventBridgeImpl<T> {
    /**
     * 创建一个 {@code T} 和 {@code E} 类型相同的 {@link EventBridge}。
     *
     * @param event Fabric API 中的 Event 对象，可通过 {@link EventFactory} 创建。
     * @param <T>   listener 的类型。
     */
    public static <T> FromFabricEvent<T, T> createIdentical(final Event<T> event) {
      return new FromFabricEvent<>(event, Function.identity(), Function.identity());
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
}
