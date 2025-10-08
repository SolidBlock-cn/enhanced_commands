package pers.solid.ecmd.configs;

import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public class RegistryParsingConfig {
  public static final RegistryParsingConfig DEFAULT = new RegistryParsingConfig();
  public static RegistryParsingConfig CURRENT = DEFAULT;

  /**
   * 解析注册表项时，返回更加详细的报错信息。
   *
   * @see pers.solid.ecmd.util.mixin.MixinShared#mixinModifiedParseThrow(RegistryKey, Supplier, LocalIntRef, StringReader, Identifier)
   */
  public boolean detailedUnknownRegistryEntry = true;
}
