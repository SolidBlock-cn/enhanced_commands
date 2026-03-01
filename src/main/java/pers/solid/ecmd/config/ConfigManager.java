package pers.solid.ecmd.config;

import com.google.gson.*;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.ecmd.EnhancedCommands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * <p>此类用于管理各配置分类的读取和保存。
 * <p>本模组的配置文件存储于：<code><游戏目录>/config/enhanced_command/<分类名称>.json</code>
 */
public final class ConfigManager {
  public static final Gson GSON = new GsonBuilder()
      .setPrettyPrinting()
      .create();
  public static final Path CONFIG_PATH = EnhancedCommands.getConfigDir().resolve("enhanced_commands");
  public static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);

  public static void loadAllConfigsFromJson() {
    ConfigCategory.REGISTRY.values().forEach(ConfigManager::readCategoryFromFile);
    ConfigCategory.REGISTRY.values().forEach(ConfigManager::saveCategoryToFile);
  }

  public static <C> void readCategoryFromFile(ConfigCategory<C> category) {
    final String name = category.name;
    final Path configPath = CONFIG_PATH.resolve(name + ".json");
    LOGGER.info("Read config for {} at {}", category.name, configPath);

    final C config = Objects.requireNonNull(ObjectUtils.clone(category.defaultConfig), "Invalid null default config copy, maybe " + category.defaultConfig.getClass() + " is not cloneable?");
    try (BufferedReader reader = Files.newBufferedReader(configPath)) {
      final JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
      category.configEntries.forEach((entryName, entry) -> readConfigEntryFromJson(entryName, entry, config, jsonObject));
    } catch (NoSuchFileException e) {
      LOGGER.info("Config file for {} does not exist, using default.", configPath);
    } catch (JsonSyntaxException e) {
      LOGGER.error("Invalid JSON in config file {}:", configPath, e);
    } catch (IOException | RuntimeException e) {
      LOGGER.error("Failed to read config file {}:", configPath, e);
    }
    category.currentConfigSetter.accept(config);
  }

  public static <C> void saveCategoryToFile(ConfigCategory<C> category) {
    final String name = category.name;
    try {
      Files.createDirectories(CONFIG_PATH);
    } catch (IOException e) {
      throw new RuntimeException("Creating file directory", e);
    }
    final Path configPath = CONFIG_PATH.resolve(name + ".json");
    try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
      final JsonObject jsonObject = new JsonObject();
      category.configEntries.forEach((entryName, entry) -> writeConfigEntryToJson(entryName, entry, category.getCurrent(), jsonObject));
      GSON.toJson(jsonObject, writer);
      category.dirty = false;
    } catch (IOException e) {
      LOGGER.error("Failed to write config file for {}:", name, e);
    }
  }

  public static <C, T> void readConfigEntryFromJson(@NotNull String name, @NotNull ConfigEntry<C, T> entry, @NotNull C config, @NotNull JsonObject jsonObject) {
    final JsonElement jsonElement = jsonObject.get(name);
    if (jsonElement != null) {
      final DataResult<T> parse = entry.type.codec().parse(JsonOps.INSTANCE, jsonElement);
      entry.setter.accept(config, parse.getOrThrow());
    }
  }

  public static <C, T> void writeConfigEntryToJson(@NotNull String name, @NotNull ConfigEntry<C, T> entry, @NotNull C config, @NotNull JsonObject jsonObject) {
    final T value = entry.getter.apply(config);
    jsonObject.add(name, entry.type.codec().encodeStart(JsonOps.INSTANCE, value).getOrThrow());
  }
}
