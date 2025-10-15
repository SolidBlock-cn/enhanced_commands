package pers.solid.ecmd.config;

import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * <p>此类用于通过反射来生成各配置分类和配置项。
 * <p>本模组的配置分类和配置项就是通过反射生成的，配置分类存储于 {@link ConfigCategories}，各配置项存储于 {@link ConfigCategory#configEntries} 中，但不存储于具体的字段。
 */
public final class ConfigReflectionHelper {
  private ConfigReflectionHelper() {
  }

  /**
   * 通过反射来创建一个配置分类，并处理好各配置项。
   * <p>配置分类的名称，将是类名称去掉后面的“Config”再转化为小写下划线形式。
   * <p>配置分类中各配置项的名称，将会是字段名称转化为小宝下划线形式。
   * <p>配置项的类型，取决于 {@link ConfigEntryType#fromClass(Class)}。
   *
   * @param configClass    配置类，该类不需要任何构造器，也不需要覆盖任何类或实现任何接口，但需要 static final 字段{@code DEFAULT} 和非 final 的 static 字段 {@code current}，且这两个字段的类型都应当为此类。
   * @param entryModifiers 用于对特定的配置项进行一些额外的修改。
   * @return 代表该类的配置分类，包括各配置项。
   * @see ConfigEntryType#fromClass
   * @see #convertCamelToUnderscore
   */
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
    final ConfigEntry<C, T> entry = category.createEntry(name, ConfigEntryType.fromClass(type), c -> {
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

  /**
   * 将字符串转化为小写下划线的形式。例如，{@code convertCamelToUnderscore("maxFlyingSpeed")} 将返回 {@code "max_flying_speed"}。
   */
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
