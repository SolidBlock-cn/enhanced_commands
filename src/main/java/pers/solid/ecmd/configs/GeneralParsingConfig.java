package pers.solid.ecmd.configs;

public class GeneralParsingConfig implements Cloneable {
  public static final GeneralParsingConfig DEFAULT = new GeneralParsingConfig();
  public static GeneralParsingConfig CURRENT = DEFAULT;

  public boolean suggestionEmitDefaultNamespace = true;
  public boolean suggestNonDefaultNamespacedIds = true;

  /**
   * 改善 NBT Path 的解析方式，在原版中，只有解析到空格，才会停止对整个 NBT Path 的解析，这是会出现一些问题的。此选项可用于修改这一行为。
   */
  public boolean improvedNbtPathParsing = true;

  @Override
  public GeneralParsingConfig clone() {
    try {
      return (GeneralParsingConfig) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
