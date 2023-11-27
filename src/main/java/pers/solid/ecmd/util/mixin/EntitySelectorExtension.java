package pers.solid.ecmd.util.mixin;

import net.minecraft.command.EntitySelector;
import pers.solid.ecmd.mixin.EntitySelectorMixin;
import pers.solid.ecmd.predicate.entity.EntitySelectorExtras;

/**
 * 此接口通过 {@link EntitySelectorMixin} 使得 {@link EntitySelector} 实现。
 */
public interface EntitySelectorExtension {

  /**
   * 获取原版的 {@link EntitySelector} 对象中，通过本模组加入的包含一些扩展信息的对象的字段。
   */
  EntitySelectorExtras ec$getExt();
}
