package pers.solid.ecmd.predicate.entity;

import net.minecraft.command.EntitySelectorReader;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.mixin.EntitySelectorReaderExtension;

/**
 * 这是在 {@link EntitySelectorReader} 中加入的一些额外的信息，用于本模组。
 */
public class EntitySelectorReaderExtras {

  /**
   * 在解析过程中，最近一次解析到的选项名称之前的位置所在的 cursor。
   */
  public int cursorBeforeOptionName;
  /**
   * 在解析过程中，最近一次解析到的选项名称之后的位置所在的 cursor。
   */
  public int cursorAfterOptionName;
  /**
   * 指“@”后的内容的类型。
   */
  public @Nullable String atVariable;

  public static EntitySelectorReaderExtras getOf(EntitySelectorReader entitySelectorReader) {
    return ((EntitySelectorReaderExtension) entitySelectorReader).ec$getExt();
  }
}
