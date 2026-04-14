package pers.solid.ecmd.api;

import com.mojang.serialization.Codec;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import pers.solid.ecmd.registry.EnhancedDynamicRegistryInfo;

/**
 * <p>此类用于连接 Fabric 和 NeoForge 的相关注册表 API。Fabric 和 NeoForge 均使用原版的注册表对象，但是其注册时间并不相同，具体包括：
 * <ul>
 *   <li>Fabric 的注册表以及其内容均在初始化过程中注册，其实现较为简单。
 *   <li>NeoForge 的略复杂些，将注册表注册到根据注册表，以及注册注册表的内容，均需要在单独的模组事件总线中注册对应事件。
 * </ul>
 * <p>因此，此类实际上包装了一个注册表对象，并提供了一些与注册表相关但在不同平台之间有区别的实用方法。在 NeoForge 中，此类还带有一个 {@code DeferredRegister}。
 *
 * @param <T> 注册表内容的类型。
 * @implNote 同一个注册表在不同模组中可以有多个不同的 {@link RegistryBridge} 对象，因为此类的实例方法是用于内容的注册，不同模组可以注册同一个注册表的不同内容。
 */
@FieldsAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface RegistryBridge<T> {
  /**
   * 创建一个注册表。在 Fabric 中，此注册表会立即创建并注册至根注册表。在 Forge 中，此注册表只会创建但没有注册，需要调用 {@link #registerToRootRegistry(Registry, InitializeContext)} 才能完成注册。
   *
   * @param key  此注册表对应的 {@link ResourceKey}，可通过原版的 {@link ResourceKey#createRegistryKey(ResourceLocation)} 注册。
   * @param sync 注册表是否需要在客户端与服务器之间同步。
   * @return 新的注册表对象。在 Fabric 中已注册至根注册表，而在 Forge 中需要调用。
   * @apiNote 在 Fabric 和 NeoForge 的共用代码中，将此方法的返回值作为静态字段，并在模组的初始化方法中调用 {@link #registerToRootRegistry(Registry, InitializeContext)}，从而确保其在 Fabric 和 NeoForge 中都被注册。
   */
  @ExpectPlatform
  static <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, boolean sync) {
    throw new AssertionError();
  }

  /**
   * 基于一个 {@link Registry} 对象，创建一个 {@link RegistryBridge} 的实例，以用于内容的注册。
   *
   * @param namespace 此对象使用的命名空间。注册表的所有注册都将使用此命名空间。
   * @param registry  已创建的 {@link Registry} 对象。可以通过 {@link #createRegistry(ResourceKey, boolean)} 创建。
   * @return 新的 {@link RegistryBridge} 实例。
   */
  @ExpectPlatform
  static <T> RegistryBridge<T> create(String namespace, Registry<T> registry) {
    throw new AssertionError();
  }

  /**
   * 使用 Fabric 和 NeoForge 各自的 API，注册动态注册表，需要先注册注册表，才能注册到动态注册表的注册表。
   *
   * @apiNote 通常来说，动态注册表是诸如魔咒、世界类型、地物类型等可通过数据加载但不可通过 {@code /reload} 重新加载的注册表，Fabric API 和 NeoForge 均提供相应注册方式。本模组还提供了 {@link EnhancedServerReloadableRegistries}，以实现注册表可通过 {@code /reload} 重新加载。
   */
  @ExpectPlatform
  static <T> void registerDynamicRegistry(ResourceKey<Registry<T>> resourceKey, Codec<T> codec, boolean sync, InitializeContext context) {
    throw new AssertionError();
  }

  /**
   * 使用 Fabric 和 NeoForge 各自的 API，注册动态注册表，需要先注册注册表，才能注册到动态注册表的注册表。
   *
   * @apiNote 通常来说，动态注册表是诸如魔咒、世界类型、地物类型等可通过数据加载但不可通过 {@code /reload} 重新加载的注册表，Fabric API 和 NeoForge 均提供相应注册方式。本模组还提供了 {@link EnhancedServerReloadableRegistries}，以实现注册表可通过 {@code /reload} 重新加载。
   */
  static <T> void registerDynamicRegistry(EnhancedDynamicRegistryInfo<T> info, boolean sync, InitializeContext context) {
    registerDynamicRegistry(info.registryKey(), info.codec(), sync, context);
  }

  /**
   * 将注册表注册到根注册表，从而使游戏内识别此注册表。在 Fabric 中不起作用，因为已经在 {@link #createRegistry(ResourceKey, boolean)} 中完成注册，而在 NeoForge 中，此方法会将注册表注册到对应事件中。
   *
   * @param registry 需要注册的注册表。
   * @param context  用于在 NeoForge 中提供 modEventBus。
   * @apiNote 尽管此方法只在 NeoForge 中起作用，但可以写在 Fabric 与 NeoForge 共用的代码中，通常也建议这么做。
   */
  @ExpectPlatform
  static void registerToRootRegistry(Registry<?> registry, InitializeContext context) {
    // no ops
    // only ops in NeoForge
  }

  /**
   * 在 Fabric 中，会检查整个注册表，如果此前有其他模组已经在此注册表注册了内容，则会视为非空，返回 {@code false}。而在 NeoForge 中，会检查其 {@code DeferredRegister} 是否为空，不受其他模组影响。
   *
   * @return 注册表或其 DeferredRegister 是否为空。
   */
  @Contract(pure = true)
  boolean isEmpty();

  /**
   * 将内容注册到此注册表。在 Fabric 中，会<i>立即</i>注册。在 NeoForge 中，会将其添加到 {@code DeferredRegister}，还需要在模组初始化中调用 {@link #registerContents(InitializeContext)} 以完成注册。
   *
   * @param name  要注册的内容的名称，即 {@link ResourceLocation#path}，不含命名空间。
   * @param value 要注册的内容。
   * @return 被注册的内容。
   * @apiNote 在完成所有对此方法的调用后，还应在模组初始化中调用 {@link #registerContents(InitializeContext)}，以确保模组在 NeoForge 中也正确注册。
   */
  <R extends T> R register(String name, R value);

  /**
   * @return 此对象所使用的 {@link Registry} 实例。
   */
  @Contract(pure = true)
  Registry<?> registry();

  /**
   * @return 此对象所使用的 {@link ResourceKey} 实例。
   */
  @Contract(pure = true)
  ResourceKey<? extends Registry<T>> key();

  /**
   * 检查此注册表（或 NeoForge 中的 {@code DeferredRegister}）是否为空，如果为空则抛出错误。
   *
   * @see #isEmpty()
   */
  default void validateNonEmpty() {
    if (isEmpty()) {
      throw new IllegalStateException("Registry " + key().registry() + " is empty!");
    }
  }

  /**
   * 在 NeoForge 中，将此对象的 {@code DeferredRegister} 注册到模组的事件总线中的相应事件中。在 Fabric 中不执行操作。
   *
   * @apiNote 通常应在模组初始化类中调用此方法，以确保注册表的内容在 Fabric 和 NeoForge 中都正确注册。
   */
  void registerContents(InitializeContext context);

  /**
   * 检查此注册表是否为空，并在 NeoForge 中注册到模组的事件总线中的相应事件中。
   *
   * @see #validateNonEmpty()
   * @see #registerContents(InitializeContext)
   */
  @ApiStatus.NonExtendable
  default void validateAndRegisterContents(InitializeContext context) {
    validateNonEmpty();
    registerContents(context);
  }
}
