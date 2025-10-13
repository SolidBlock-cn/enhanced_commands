package pers.solid.ecmd.config;

import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.function.Supplier;

public class GeneralParsingConfig implements Cloneable {
  public static final GeneralParsingConfig DEFAULT = new GeneralParsingConfig();
  public static GeneralParsingConfig current = DEFAULT;

  /**
   * 在显示 id 的建议时，省略默认的命名空间，例如在输入方块 id 时，将提示 {@code oak_planks} 而非 {@code minecraft:oak_planks}。
   */
  public boolean suggestionEmitDefaultNamespace = true;

  /**
   * 在显示 id 的建议时，如果没有输入命名空间，也会提供非原版命名空间的建议。例如在输入生物群系时，如果输入了 {@code #is_snowy}，将提示 {@code #c:is_snowy}。
   */
  public boolean suggestNonDefaultNamespacedIds = true;

  /**
   * 在解析 id 时，如果有大写字母，将正常解析下去，并显示相应的错误信息，而不是直接提示未知的参数。
   */
  public boolean improvedIdParsing = true;

  /**
   * 在解析 id 时，如果 id 存在错误，将显示更为详细的错误信息，包括提示 id 中可能出现了大写字母等情况。
   */
  public boolean detailedIdentifierException = true;

  /**
   * 改善 NBT Path 的解析方式，在原版中，只有解析到空格，才会停止对整个 NBT Path 的解析，这是会出现一些问题的。此选项可用于修改这一行为。
   */
  public boolean improvedNbtPathParsing = true;

  /**
   * 在解析游戏模式时，允许使用其别称，适用于 {@code /gamemode} 命令和实体选择器的 {@code gamemode} 参数。
   */
  public boolean acceptGameModeAlias = true;

  /**
   * 解析注册表项时，显示更加详细的报错信息。
   *
   * @see MixinShared#mixinModifiedParseThrow(RegistryKey, Supplier, LocalIntRef, StringReader, Identifier)
   */
  public boolean detailedUnknownRegistryEntry = true;

  @Override
  public GeneralParsingConfig clone() {
    try {
      return (GeneralParsingConfig) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
