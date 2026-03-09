package pers.solid.ecmd.api;


/**
 * 包含在不同加载器特有的参数的对象。目前在 Fabric 中无，在 NeoForge 中仅有一个模组事件总线。此类是考虑到一些在 Fabric 和 NeoForge 间共用的 API 在 NeoForge 的实现需要使用到模组的事件总线，即 NeoForge 的模组主类的构造函数的参数。
 */
public interface InitializeContext {
}
