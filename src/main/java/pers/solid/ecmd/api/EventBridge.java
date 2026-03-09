package pers.solid.ecmd.api;

import net.minecraft.resources.ResourceLocation;

/**
 * <p>本模组中，共用于 Fabric 和 NeoForge 的事件系统，其底层实现是基于 Fabric API 的 {@code Event} 和 NeoForge 的 {@code EventBus}。此 API 的结构更接近 Fabric，因此在 Fabric 中的实现更为顺畅，而在 NeoForge 中实现则需要经历多次转化。
 * <p>要为此事件注册 listener，请调用 {@link #register}。要执行此事件，请调用 {@link #invoker()}，并调用其返回的值。目前还不支持设置 listener 的顺序或优先级。
 * <p>本模组中使用的 EventBridge，请见 {@link EventBridges}。关于其在 Fabric 和 NeoForge 的具体实现方式，请参见此类的子类。
 *
 * @param <T> 用于 listener 的接口，通常是一个函数式接口，其抽象方法的参数是该方法的参数，可以返回值，也可以不返回值（即返回 {@code void}）。
 * @see EventBridges
 */
public interface EventBridge<T> {

  /**
   * 返回 invoker 实例。invoker 是指会依次执行所有（或部分）已注册的 listener 的对象，其类型和各 listener 一样均为 T。
   *
   * @return invoker 实例。
   */
  T invoker();

  /**
   * 为此事件注册 listener。
   */
  void register(T listener);

  /**
   * 为此事件注册带有特定的 phase 名称的 listener，通常只在 Fabric 下才能完全生效，因为需要调用 Fabric API 中的 {@code addPhaseOrdering}。
   */
  default void register(ResourceLocation phase, T listener) {
    register(listener);
  }
}
