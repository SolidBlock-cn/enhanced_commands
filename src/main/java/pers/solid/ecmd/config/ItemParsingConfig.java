package pers.solid.ecmd.config;

import net.minecraft.commands.arguments.item.ItemParser;
import pers.solid.ecmd.config.annotations.ConfigEntryScope;

@ConfigEntryScope(ConfigEntryScopeType.BOTH)
public class ItemParsingConfig implements Cloneable {
  public static final ItemParsingConfig DEFAULT = new ItemParsingConfig();
  public static ItemParsingConfig current = DEFAULT;

  /**
   * 在原版中，输入物品组件 ID 时，如果已经输入了部分 ID，仍会在 ID 的开始处提供移除 ID 的建议，即感叹号（{@link ItemParser#SYNTAX_REMOVED_COMPONENT}）。此配置可修复此问题。
   */
  public boolean fixComponentRemovalSuggestion = true;

  @Override
  public ItemParsingConfig clone() {
    try {
      return (ItemParsingConfig) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
