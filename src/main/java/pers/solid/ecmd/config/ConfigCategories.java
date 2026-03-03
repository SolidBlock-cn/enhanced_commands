package pers.solid.ecmd.config;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

/**
 * 本模组所使用的所有配置项。
 */
public final class ConfigCategories {
  public static final ConfigCategory<CommandsConfig> COMMANDS = register(CommandsConfig.class);
  public static final ConfigCategory<EntitySelectorConfig> ENTITY_SELECTOR = register(EntitySelectorConfig.class);
  public static final ConfigCategory<GeneralParsingConfig> GENERAL = register(GeneralParsingConfig.class);
  public static final ConfigCategory<DebugConfig> DEBUG = register(DebugConfig.class);
  public static final ConfigCategory<BlockOperationConfig> BLOCK_OPERATION = register(BlockOperationConfig.class);

  private ConfigCategories() {
  }

  private static @NotNull <C> ConfigCategory<C> register(Class<C> configClass) {
    return register(configClass, Collections.emptyMap());
  }

  private static @NotNull <C> ConfigCategory<C> register(Class<C> configClass, Map<String, ConfigCategory.EntryModifier<C, ?>> entryModifiers) {
    final ConfigCategory<C> configCategory = ConfigReflectionHelper.createFromReflection(configClass, entryModifiers);
    ConfigCategory.REGISTRY.put(configCategory.name, configCategory);
    return configCategory;
  }

  public static void init() {
    Validate.notEmpty(ConfigCategory.REGISTRY, "Config category registry of Enhanced Commands mod is empty!");
  }
}
