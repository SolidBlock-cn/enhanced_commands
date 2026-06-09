package pers.solid.ecmd.config;

import net.minecraft.ChatFormatting;
import pers.solid.ecmd.config.annotations.ConfigEntryScope;
import pers.solid.ecmd.config.annotations.OverrideDescription;
import pers.solid.ecmd.config.annotations.TextEntry;
import pers.solid.ecmd.config.annotations.TextInfo;

@ConfigEntryScope(ConfigEntryScopeType.BOTH)
public class EntitySelectorParsingConfig implements Cloneable {
  public static final EntitySelectorParsingConfig DEFAULT = new EntitySelectorParsingConfig();
  public static EntitySelectorParsingConfig current = DEFAULT;

  /**
   * 在读取实体选择器时，如果遇到可识别但因某些原因不可应用的选项名称，则尝试详细描述其原因，而非仅表明某选项不适用于这里。
   */
  public boolean detailedInapplicableEntitySelectorOption = true;

  /**
   * 在实体选择器中输入选项时，允许输入别称，例如 c、m 等。
   */
  @OverrideDescription(@TextInfo(args = {
      @TextEntry(value = "@a[c=1]", formatting = ChatFormatting.GRAY),
      @TextEntry(value = "@a[limit=1]", formatting = ChatFormatting.GRAY)
  }))
  public boolean acceptOptionNameAlias = true;

  /**
   * 实体选择器的 level 参数允许取反。
   */
  @OverrideDescription(@TextInfo(args = {
      @TextEntry(value = "level", formatting = ChatFormatting.GRAY),
      @TextEntry(value = "@a[level=!2]", formatting = ChatFormatting.GRAY)
  }))
  public boolean allowLevelInversion = true;

  /**
   * 在解析 {@code @p} 时，如果没有指定 {@code sort} 参数，则允许使用负值来表示最远的实体。
   */
  @OverrideDescription(@TextInfo(args = {
      @TextEntry(value = "@p", formatting = ChatFormatting.GRAY),
      @TextEntry(value = "sort", formatting = ChatFormatting.GRAY),
      @TextEntry(value = "@p[limit=-1]", formatting = ChatFormatting.GRAY),
      @TextEntry(value = "@a[limit=1, sort=furthest]", formatting = ChatFormatting.GRAY),
  }))
  public boolean allowNegativeDistanceForNearest = true;

  /**
   * 允许使用像 {@code gamemode=creative|adventure} 这样的方式选择多个游戏模式。
   */
  @OverrideDescription(@TextInfo(args = {
      @TextEntry(value = "gamemode=creative|adventure", formatting = ChatFormatting.GRAY),
  }))
  public boolean allowMultipleGameModes = true;

  /**
   * 在显示实体类型 id 的建议时，同时提示其名称，鼠标悬浮在建议项时显示。
   */
  public boolean improveEntityTypeSuggestion = true;

  /**
   * 允许使用像 {@code type=cat|dog} 这样的方式选择多个类型。
   */
  @OverrideDescription(@TextInfo(args = {
      @TextEntry(value = "@e[type=cat|dog]", formatting = ChatFormatting.GRAY)
  }))
  public boolean allowMultipleTypes = true;

  /**
   * 输入实体类型标签时，避免未输入完成就解析进入下一步导致的建议不显示的问题。
   */
  public boolean fixEntityTypeTagSuggestions = true;

  /**
   * 在实体选择器中输入记分项时，提供记分项的建议。
   */
  @OverrideDescription(@TextInfo(args = {
      @TextEntry(value = "@a[scores={§n" + "objective§r=value}]", formatting = ChatFormatting.GRAY)
  }))
  public boolean showScoreObjectiveSuggestions = true;

  /**
   * 在输入分数时，允许将分数的预期值取反，例如 {@code @a[scores={a=!1}]}。
   */
  @OverrideDescription(@TextInfo(args = {
      @TextEntry(value = "@a[scores={a=!1}]", formatting = ChatFormatting.GRAY),
  }))
  public boolean allowScoreInversion = true;

  /**
   * 在实体选择器中输入进度时，提供进度 id 的建议。
   */
  @OverrideDescription(@TextInfo(args = {
      @TextEntry(value = "@a[advancements={§n" + "advancement_id§r=value}]", formatting = ChatFormatting.GRAY)
  }))
  public boolean showAdvancementsSuggestions = true;

  /**
   * 在实体选择器中输入进度准则时，提供进度准则名称的建议。
   */
  @OverrideDescription(@TextInfo(args = {
      @TextEntry(value = "@a[advancements={advancement_id={§n" + "criterion§r=value}}]", formatting = ChatFormatting.GRAY)
  }))
  public boolean showAdvancementsCriterionSuggestions = true;

  /**
   * 在实体选择器中输入进度准则的名称时，允许使用带有引号的字符串，从而避免含有空格等特殊字符的准则名称无法使用的问题。
   */
  @OverrideDescription(@TextInfo(args = {
      @TextEntry(value = "@a[advancements={advancement_id={\"quoted criterion\"=true}}]", formatting = ChatFormatting.GRAY)
  }))
  public boolean acceptQuotedAdvancementCriterionName = true;

  /**
   * 在实体选择器中输入战利品表谓词（即 predicate 选项）时，提供谓词 id 的建议。
   */
  @OverrideDescription(@TextInfo(args = {
      @TextEntry(value = "predicate", formatting = ChatFormatting.GRAY)
  }))
  public boolean showPredicateSuggestions = true;

  /**
   * 在实体选择器中输入战利品表谓词时，允许直接输入 json 指定谓词，而不是使用其 id。
   */
  public boolean allowLiteralPredicateJson = true;

  @Override
  public EntitySelectorParsingConfig clone() {
    try {
      return (EntitySelectorParsingConfig) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
