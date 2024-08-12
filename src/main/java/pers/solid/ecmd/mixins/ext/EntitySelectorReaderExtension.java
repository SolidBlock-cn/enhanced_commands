package pers.solid.ecmd.mixins.ext;

import net.minecraft.command.EntitySelectorReader;
import org.jetbrains.annotations.Contract;
import pers.solid.ecmd.mixins.mixin.EntitySelectorReaderMixin;
import pers.solid.ecmd.predicate.entity.EntitySelectorReaderExtras;

/**
 * 此接口通过 {@link EntitySelectorReaderMixin} 使得 {@link EntitySelectorReader} 实现。
 */
public interface EntitySelectorReaderExtension {
  /**
   * 获取原版的 {@link EntitySelectorReader} 对象中，通过本模组加入的包含一些扩展信息的对象的字段。
   */
  @Contract(pure = true)
  default EntitySelectorReaderExtras extension$ec() {
    throw new UnsupportedOperationException();
  }
}
