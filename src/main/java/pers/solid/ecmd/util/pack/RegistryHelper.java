package pers.solid.ecmd.util.pack;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.ResourceKey;

public final class RegistryHelper {
  private RegistryHelper() {
  }

  /**
   * <p>创建在任何情况下都可正确序列化为 ID 的 {@link Holder.Reference}。
   * <p>通常情况下，在序列化 {@link Holder.Reference} 时，会检查其 owner 是否匹配。在数据生成过程中，直接使用 {@link Holder.Reference#createStandAlone(HolderOwner, ResourceKey)} 创建的对象可能无法正常被序列化，因为其 owner 可能不匹配。此方法返回的对象使用 {@link #safeHolderOwner()}，使之始终能够序列化。
   * <pre>{@code
   * RegistryHelper.safeStandAloneHolderReference(Enchantments.LOOTING);
   * // 此对象转化为 NBT 或 JSON 后将成为字符串 "minecraft:looting"
   * }</pre>
   *
   * @return 在任何情况下都可正确序列化为 ID 的 {@link Holder.Reference}。
   */
  public static <T> Holder.Reference<T> safeStandAloneHolderReference(ResourceKey<T> key) {
    return Holder.Reference.createStandAlone(safeHolderOwner(), key);
  }

  /**
   * <p>通常情况下，在序列化 {@link Holder.Reference} 时，会检查其 owner 是否匹配。在数据生成过程中，使用 {@link Holder.Reference#createStandAlone(HolderOwner, ResourceKey)} 创建的对象可能无法正常被序列化，因为其 owner 可能不匹配。此方法返回的 owner 在 {@link Holder} 中始终能被正确序列化。
   *
   * @return 始终能允许序列化的 {@link HolderOwner}。
   * @see #safeStandAloneHolderReference(ResourceKey)
   */
  @SuppressWarnings("unchecked")
  public static <T> HolderOwner<T> safeHolderOwner() {
    return (HolderOwner<T>) SafeHolderOwner.VALUE;
  }

  public enum SafeHolderOwner implements HolderOwner<Object> {
    VALUE;

    @Override
    public boolean canSerializeIn(HolderOwner<Object> owner) {
      return true;
    }
  }
}
