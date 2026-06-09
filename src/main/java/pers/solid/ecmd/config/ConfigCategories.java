package pers.solid.ecmd.config;

import org.apache.commons.lang3.Validate;

import java.util.Collections;
import java.util.Map;

/**
 * 本模组所使用的所有配置项。
 */
public final class ConfigCategories {
  public static final ConfigCategory<BlockOperationConfig> BLOCK_OPERATION = register(BlockOperationConfig.class);
  public static final ConfigCategory<CommandsConfig> COMMANDS = register(CommandsConfig.class);
  public static final ConfigCategory<EntitySelectorParsingConfig> ENTITY_SELECTOR = register(EntitySelectorParsingConfig.class);
  public static final ConfigCategory<GameplayConfig> GAMEPLAY = register(GameplayConfig.class);
  public static final ConfigCategory<GeneralParsingConfig> GENERAL_PARSING = register(GeneralParsingConfig.class);
  public static final ConfigCategory<ItemParsingConfig> ITEM_PARSING = register(ItemParsingConfig.class);
  public static final ConfigCategory<DebugConfig> DEBUG = register(DebugConfig.class);

  private ConfigCategories() {
  }

  private static <C> ConfigCategory<C> register(Class<C> configClass) {
    return register(configClass, Collections.emptyMap());
  }

  private static <C> ConfigCategory<C> register(Class<C> configClass, Map<String, ConfigCategory.EntryModifier<C, ?>> entryModifiers) {
    final ConfigCategory<C> configCategory = ConfigReflectionHelper.createCategoryFromReflection(configClass, entryModifiers);
    ConfigCategory.REGISTRY.put(configCategory.name, configCategory);
    return configCategory;
  }

  public static void init() {
    Validate.notEmpty(ConfigCategory.REGISTRY, "Config category registry of Enhanced Commands mod is empty!");
  }
}
