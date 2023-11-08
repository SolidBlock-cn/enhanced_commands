package pers.solid.ecmd.util.mixin;

import net.minecraft.command.EntitySelectorReader;
import pers.solid.ecmd.predicate.entity.EntitySelectorReaderExtras;

/**
 * 此接口通过 {@link pers.solid.ecmd.mixin.EntitySelectorReaderMixin} 使得 {@link EntitySelectorReader} 实现。
 */
public interface EntitySelectorReaderExtension {
  /**
   * 获取原版的 {@link EntitySelectorReader} 对象中，通过本模组加入的包含一些扩展信息的对象的字段。
   */
  EntitySelectorReaderExtras ec$getExt();
}
