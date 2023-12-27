package pers.solid.ecmd.configs;

public class EntitySelectorParsingConfig {
  public static final EntitySelectorParsingConfig DEFAULT = new EntitySelectorParsingConfig();
  public static final EntitySelectorParsingConfig CURRENT = DEFAULT;

  /**
   * 在读取实体选择器时，如果遇到可识别但因某些原因不可应用的选项名称，则尝试详细描述其原因，而非仅表明某选项不适用于这里。
   */
  public boolean detailedInapplicableEntitySelectorOption = true;

  /**
   * 在实体选择器中输入选项时，允许输入别称，例如 c、m 等。
   */
  public boolean acceptOptionNameAlias = true;

  /**
   * 实体选择器的 level 参数允许取反。
   */
  public boolean allowLevelInversion = true;

  /**
   * 在解析 {@code @p} 时，如果没有指定 {@code sort} 参数，则允许使用负值来表示最远的实体。
   */
  public boolean allowNegativeDistanceForNearest = true;

  /**
   * 在解析游戏模式时，允许使用其别称。
   */
  public boolean acceptGameModeAlias = true;

  /**
   * 允许使用像 {@code gamemode=creative|adventure} 这样的方式选择多个游戏模式。
   */
  public boolean allowMultipleGameModes = true;

  /**
   * 输入实体类型标签时，避免未输入完成就解析进入下一步导致的建议不显示的问题。
   */
  public boolean fixEntityTypeTagSuggestions = true;

  /**
   * 在实体选择器中输入记分项时，提供记分项的建议。
   */
  public boolean showScoreObjectiveSuggestions = true;

  /**
   * 在输入分数时，允许将分数的预期值取反，例如 {@code = scores={a=!1}}。
   */
  public boolean allowScoreInversion = true;

  /**
   * 在实体选择器中输入进度时，提供进度 id 的建议。
   */
  public boolean showAdvancementsSuggestions = true;

  /**
   * 在实体选择器中输入进度条件时，提供进度条件名称的建议。
   */
  public boolean showAdvancementsCriterionSuggestions = true;

  /**
   * 在实体选择器中输入进度条件的名称时，允许使用带有引号的字符串，从而避免含有空格等特殊字符的条件名称无法使用的问题。
   */
  public boolean acceptQuotedAdvancementCriterionName = true;

  /**
   * 在实体选择器中输入战利品表谓词时，提供谓词 id 的建议。
   */
  public boolean showPredicateSuggestions = true;

  /**
   * 在实体选择器中输入战利品表谓词时，允许直接输入 json 指定谓词，而不是使用其 id。
   */
  public boolean allowLiteralPredicateJson = true;
}
