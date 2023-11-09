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
  /**
   * 如果是通过 @r、@p 等选择器指定的实体类型，则为 {@code true}。在这种情况下，应该允许指定一个 type 参数来覆盖此设置。
   */
  public boolean implicitEntityType = false;
  /**
   * 如果是通过 @r、@p 等选择器指定的 {@link EntitySelectorReader#setIncludesNonPlayers(boolean)}，且没有通过 {@code gamemode} 等参数指定，则此字段为 true。
   */
  public boolean implicitNonPlayers = false;
  /**
   * 如果使用了 {@code r} 和 {@code rm} 指定的距离，而非{ @code distance} 选项，则为 {@code true}。
   */
  public boolean implicitDistance = false;
  /**
   * 如果使用了 {@code @p} 搭配负 {@code limit} 值，则为 {@code true}。
   */
  public boolean implicitNegativeLimit = false;

  public static EntitySelectorReaderExtras getOf(EntitySelectorReader entitySelectorReader) {
    return ((EntitySelectorReaderExtension) entitySelectorReader).ec$getExt();
  }
}
