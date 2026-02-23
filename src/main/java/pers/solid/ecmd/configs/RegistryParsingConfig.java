package pers.solid.ecmd.configs;

import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class RegistryParsingConfig {
  public static final RegistryParsingConfig DEFAULT = new RegistryParsingConfig();
  public static RegistryParsingConfig CURRENT = DEFAULT;

  /**
   * 解析注册表项时，返回更加详细的报错信息。
   *
   * @see pers.solid.ecmd.util.mixin.MixinShared#mixinModifiedParseThrow(ResourceKey, Supplier, LocalIntRef, StringReader, ResourceLocation)
   */
  public boolean detailedUnknownRegistryEntry = true;
}
