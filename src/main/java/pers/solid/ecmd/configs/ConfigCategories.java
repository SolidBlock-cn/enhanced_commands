package pers.solid.ecmd.configs;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public final class ConfigCategories {
  public static final ConfigCategory<CommandsConfig> COMMANDS = register(CommandsConfig.class, Map.of(
      "max_history_count", (ConfigCategory.EntryModifier<CommandsConfig, Integer>) c -> c.setValueValidator(integer -> {
        if (integer < 0) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().create(integer, 0);
        } else if (integer > 32767) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().create(integer, 32767);
        }
      })
  ));
  public static final ConfigCategory<EntitySelectorParsingConfig> ENTITY_SELECTOR_PARSING = register(EntitySelectorParsingConfig.class);
  public static final ConfigCategory<GeneralParsingConfig> GENERAL = register(GeneralParsingConfig.class);
  public static final ConfigCategory<RegistryParsingConfig> REGISTRY_PARSING = register(RegistryParsingConfig.class);

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
    Validate.notEmpty(ConfigCategory.REGISTRY, "Enhanced Commands mod: Config category registry is empty");
  }
}
