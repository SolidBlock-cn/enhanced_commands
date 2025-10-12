package pers.solid.ecmd.configs;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

public final class ConfigCategories {
  public static final ConfigCategory<CommandsConfig> COMMANDS = register(CommandsConfig.class);
  public static final ConfigCategory<EntitySelectorParsingConfig> ENTITY_SELECTOR_PARSING = register(EntitySelectorParsingConfig.class);
  public static final ConfigCategory<GeneralParsingConfig> GENERAL = register(GeneralParsingConfig.class);
  public static final ConfigCategory<RegistryParsingConfig> REGISTRY_PARSING = register(RegistryParsingConfig.class);

  private ConfigCategories() {
  }

  private static @NotNull <C> ConfigCategory<C> register(Class<C> configClass) {
    final ConfigCategory<C> configCategory = ConfigReflectionHelper.createFromReflection(configClass);
    ConfigCategory.REGISTRY.put(configCategory.name, configCategory);
    return configCategory;
  }

  public static void init() {
    Validate.notEmpty(ConfigCategory.REGISTRY, "Enhanced Commands mod: Config category registry is empty");
  }
}
