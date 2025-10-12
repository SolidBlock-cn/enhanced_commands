package pers.solid.ecmd.configs;

import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ConfigEntry<C, T> {
  public final @NotNull ConfigCategory<C> category;
  public final @NotNull ConfigEntryType<T> type;
  public final @NotNull String name;
  public final @NotNull Text displayName;
  public final @Nullable Text description;
  public final @NotNull Function<C, T> getter;
  public final @NotNull BiConsumer<C, T> setter;
  public final @NotNull T defaultValue;

  protected ConfigEntry(@NotNull ConfigCategory<C> category, @NotNull ConfigEntryType<T> type, @NotNull String name, @NotNull Text displayName, @Nullable Text description, @NotNull Function<C, T> getter, @NotNull BiConsumer<C, T> setter, @NotNull T defaultValue) {
    this.category = category;
    this.type = type;
    this.name = name;
    this.displayName = displayName;
    this.description = description;
    this.getter = getter;
    this.setter = setter;
    this.defaultValue = defaultValue;
  }

  protected ConfigEntry(@NotNull ConfigCategory<C> category, @NotNull ConfigEntryType<T> type, @NotNull String name, @NotNull Function<C, T> getter, @NotNull BiConsumer<C, T> setter, @NotNull T defaultValue) {
    this(category, type, name, Text.translatable("enhanced_commands.config." + category.name + "." + name + ".name"), Text.translatable("enhanced_commands.config." + category.name + "." + name + ".description"), getter, setter, defaultValue);
  }

  public T getCurrent() {
    return getter.apply(category.getCurrent());
  }

  public void setCurrent(T value) {
    setter.accept(category.getCurrent(), value);
  }
}
