package pers.solid.ecmd.config;

import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public final class ConfigReflectionHelper {
  private ConfigReflectionHelper() {
  }

  public static <C> ConfigCategory<C> createFromReflection(Class<C> configClass, Map<String, ConfigCategory.EntryModifier<C, ?>> entryModifiers) {
    final String categoryName = convertCamelToUnderscore(StringUtils.removeEnd(configClass.getSimpleName(), "Config"));

    // 加载默认配置
    final C defaultConfig = getDefaultConfigFromClass(configClass);
    final Field currentConfigField;
    try {
      currentConfigField = getCurrentConfigFieldFromClass(configClass);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Get CURRENT_CONFIG field", e);
    }
    final ConfigCategory<C> category = ConfigCategory.create(categoryName, defaultConfig, () -> getCurrentConfigFromField(currentConfigField, configClass), s -> {
      try {
        currentConfigField.set(null, s);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }, !configClass.isAnnotationPresent(ConfigEntry.NoDescription.class));


    // 从类的字段中读取配置项
    for (Field field : configClass.getDeclaredFields()) {
      final int modifiers = field.getModifiers();
      if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers)) {
        continue;
      }
      final String name = convertCamelToUnderscore(field.getName());
      createEntryForField(field, category, name, defaultConfig, entryModifiers);
    }

    return category;
  }

  private static <C> @NotNull C getDefaultConfigFromClass(Class<C> configClass) {
    final C defaultConfig;
    try {
      Field field = getDefaultConfigFieldFromClass(configClass);
      Object o = field.get(null);
      if (configClass.isInstance(o)) {
        defaultConfig = configClass.cast(o);
      } else {
        throw new IllegalStateException("Default config for " + configClass.getSimpleName() + " is not type of " + configClass.getSimpleName());
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to load default config for " + configClass.getSimpleName(), e);
    }
    return defaultConfig;
  }

  private static <C> @NotNull Field getDefaultConfigFieldFromClass(Class<C> configClass) throws NoSuchFieldException, IllegalAccessException {
    final Field field = configClass.getField("DEFAULT");
    final int modifiers = field.getModifiers();
    if (Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
      return field;
    } else {
      throw new IllegalStateException("Default config for " + configClass.getSimpleName() + " is not public static final");
    }
  }

  private static <C> @NotNull C getCurrentConfigFromField(Field field, Class<C> configClass) {
    final C currentConfig;
    try {
      Object o = field.get(null);
      if (configClass.isInstance(o)) {
        currentConfig = configClass.cast(o);
      } else {
        throw new IllegalStateException("Current config for " + configClass.getSimpleName() + " is not type of " + configClass.getSimpleName());
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to load default config for " + configClass.getSimpleName(), e);
    }
    return currentConfig;
  }

  private static <C> @NotNull Field getCurrentConfigFieldFromClass(Class<C> configClass) throws NoSuchFieldException, IllegalAccessException {
    final Field field = configClass.getField("current");
    final int modifiers = field.getModifiers();
    if (!Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
      return field;
    } else {
      throw new IllegalStateException("Current config for " + configClass.getSimpleName() + " is not public static non-final");
    }
  }

  @SuppressWarnings("unchecked")
  private static <C, T> void createEntryForField(@NotNull Field field, @NotNull ConfigCategory<C> category, @NotNull String name, C defaultConfig, Map<String, ConfigCategory.EntryModifier<C, ?>> builderModifiers) {
    final Class<T> type = (Class<T>) field.getType();
    final ConfigEntry<C, T> entry = category.createEntry(name, ConfigEntryTypes.fromClass(type), c -> {
      try {
        return (T) field.get(c);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Get config value", e);
      }
    }, (c, v) -> {
      try {
        field.set(c, v);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Set config value", e);
      }
    }, Util.make(() -> {
      try {
        return (T) field.get(defaultConfig);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Get default config value", e);
      }
    }), (ConfigCategory.EntryModifier<C, T>) builderModifiers.get(name), !field.isAnnotationPresent(ConfigEntry.NoDescription.class));
    category.configEntries.put(name, entry);
  }

  public static String convertCamelToUnderscore(String s) {
    final StringBuilder b = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isUpperCase(c)) {
        if (i != 0) b.append('_');
        b.append(Character.toLowerCase(c));
      } else {
        b.append(c);
      }
    }
    return b.toString();
  }
}
