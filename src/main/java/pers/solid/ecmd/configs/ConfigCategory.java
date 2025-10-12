package pers.solid.ecmd.configs;

import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConfigCategory<C> {
  public static final Map<String, ConfigCategory<?>> REGISTRY = new LinkedHashMap<>();
  public final @NotNull String name;
  public final @NotNull Text displayName;
  public final @Nullable Text description;
  public final C defaultConfig;
  public final Supplier<C> currentConfigGetter;
  public final Consumer<C> currentConfigSetter;
  public final Map<String, ConfigEntry<C, ?>> configEntries = new LinkedHashMap<>();
  protected boolean dirty = false;

  public ConfigCategory(@NotNull String name, @NotNull Text displayName, @Nullable Text description, C defaultConfig, Supplier<C> currentConfigGetter, Consumer<C> currentConfigSetter) {
    this.name = name;
    this.displayName = displayName;
    this.description = description;
    this.defaultConfig = defaultConfig;
    this.currentConfigGetter = currentConfigGetter;
    this.currentConfigSetter = currentConfigSetter;
  }

  public ConfigCategory(@NotNull String name, C defaultConfig, Supplier<C> currentConfigGetter, Consumer<C> currentConfigSetter) {
    this(name, Text.translatable("enhanced_commands.config." + name + ".name"), Text.translatable("enhanced_cCommands.config." + name + ".description"), defaultConfig, currentConfigGetter, currentConfigSetter);
  }

  public <T> ConfigEntry<C, T> createAndRecordEntry(@NotNull String name, @NotNull ConfigEntryType<T> type, @NotNull Function<C, T> getter, @NotNull BiConsumer<C, T> setter, @NotNull T defaultValue) {
    final ConfigEntry<C, T> configEntry = new ConfigEntry<>(this, type, name, getter, setter, defaultValue);
    configEntries.put(name, configEntry);
    return configEntry;
  }

  public C getCurrent() {
    return currentConfigGetter.get();
  }

  /**
   * 将此类别的配置标记为 dirty，表示离开服务器时需要保存。
   */
  public void markDirty() {
    this.dirty = true;
  }

  public boolean isDirty() {
    return this.dirty;
  }
}
