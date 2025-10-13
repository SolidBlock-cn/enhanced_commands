package pers.solid.ecmd.config;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ConfigEntry<C, T> {
  private static final FailableConsumer<Object, CommandSyntaxException> NO_OP_VALIDATOR = object -> {};
  public final @NotNull ConfigCategory<C> category;
  public final @NotNull ConfigEntryType<T> type;
  public final @NotNull String name;
  public final @NotNull Text displayName;
  public final @Nullable Text description;
  public final @NotNull Function<C, T> getter;
  public final @NotNull BiConsumer<C, T> setter;
  protected final @NotNull FailableConsumer<? super T, CommandSyntaxException> valueValidator;
  public final @NotNull T defaultValue;

  private ConfigEntry(@NotNull ConfigCategory<C> category, @NotNull ConfigEntryType<T> type, @NotNull String name, @NotNull Text displayName, @Nullable Text description, @NotNull Function<C, T> getter, @NotNull BiConsumer<C, T> setter, @NotNull FailableConsumer<? super T, CommandSyntaxException> valueValidator, @NotNull T defaultValue) {
    this.category = category;
    this.type = type;
    this.name = name;
    this.displayName = displayName;
    this.description = description;
    this.getter = getter;
    this.setter = setter;
    this.valueValidator = valueValidator;
    this.defaultValue = defaultValue;
  }

  public T getCurrent() {
    return getter.apply(category.getCurrent());
  }

  public void setCurrent(T value) throws CommandSyntaxException {
    valueValidator.accept(value);
    setter.accept(category.getCurrent(), value);
  }

  public static <C, T> Builder<C, T> builder(ConfigCategory<C> category, ConfigEntryType<T> type, String name) {
    return new Builder<>(category, type, name);
  }

  public static class Builder<C, T> {
    private final ConfigCategory<C> category;
    private final ConfigEntryType<T> type;
    private final String name;
    private final String displayNameTranslationKey;
    private final String descriptionTranslationKey;
    private Text displayName;
    private @Nullable Text description;
    private Function<C, T> getter;
    private BiConsumer<C, T> setter;
    private FailableConsumer<? super T, CommandSyntaxException> valueValidator = NO_OP_VALIDATOR;
    private T defaultValue;

    public Builder(ConfigCategory<C> category, ConfigEntryType<T> type, String name) {
      this.category = category;
      this.type = type;
      this.name = name;
      this.displayNameTranslationKey = "enhanced_commands.config." + category.name + "." + name;
      this.descriptionTranslationKey = "enhanced_commands.config." + category.name + "." + name + ".description";
    }

    public Builder<C, T> setDisplayName(@NotNull Text displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder<C, T> setDisplayName(@NotNull Function<@NotNull String, @NotNull Text> translationKeyToText) {
      this.displayName = translationKeyToText.apply(displayNameTranslationKey);
      return this;
    }

    public Builder<C, T> setDescription(@Nullable Text description) {
      this.description = description;
      return this;
    }

    public Builder<C, T> setDescription(@NotNull Function<@NotNull String, @Nullable Text> translationKeyToText) {
      this.description = translationKeyToText.apply(descriptionTranslationKey);
      return this;
    }

    public Builder<C, T> appendDescription(@Nullable Text description) {
      if (this.description == null) {
        this.description = description;
      } else if (description != null) {
        this.description = Text.empty()
            .append(this.description)
            .append(ScreenTexts.LINE_BREAK)
            .append(description);
      }
      return this;
    }

    public Builder<C, T> setGetter(@NotNull Function<C, T> getter) {
      this.getter = getter;
      return this;
    }

    public Builder<C, T> setSetter(@NotNull BiConsumer<C, T> setter) {
      this.setter = setter;
      return this;
    }

    public Builder<C, T> setValueValidator(@NotNull FailableConsumer<? super T, CommandSyntaxException> valueValidator) {
      this.valueValidator = valueValidator;
      return this;
    }

    public Builder<C, T> setDefaultValue(@NotNull T defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public ConfigEntry<C, T> build() {
      Preconditions.checkNotNull(displayName, "displayName");
      Preconditions.checkNotNull(getter, "getter");
      Preconditions.checkNotNull(setter, "setter");
      Preconditions.checkNotNull(defaultValue, "defaultValue");
      return new ConfigEntry<>(category, type, name, displayName, description, getter, setter, valueValidator, defaultValue);
    }
  }

  @Target({ElementType.TYPE, ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface NoDescription {}
}
