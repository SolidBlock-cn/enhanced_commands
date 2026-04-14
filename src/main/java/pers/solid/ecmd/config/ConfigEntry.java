package pers.solid.ecmd.config;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.FailableConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * <p>配置项，对该配置项调用相应方法可设置或修改配置的值。每个配置项都可以有各自的名称和描述。创建配置项时，请使用 {@link #builder}。如需利用反射来快速为字段创建配置项，请使用 {@link ConfigReflectionHelper}。
 * <p>本模组的各配置分类的配置项均通过反射生成，且不存储在专门的字段。
 *
 * @param <C> 配置实例的类，例如 {@link BlockOperationConfig}。
 * @param <T> 该配置的项的具体的值的类型，例如 int 或 boolean。
 * @see ConfigReflectionHelper
 */
public class ConfigEntry<C, T> {
  /**
   * 在验证值时使用该参数，表示该配置项接受对应类型的任何值。
   */
  private static final FailableConsumer<Object, CommandSyntaxException> NO_OP_VALIDATOR = object -> {};
  /**
   * 配置项所在的分类。
   */
  public final ConfigCategory<C> category;
  /**
   * 配置项的类型，将影响着这个配置项的值的类型，进而影响着其值的呈现方式以及在命令中的输入方式。
   */
  public final ConfigEntryType<T> type;
  /**
   * 配置项的名称，用于命令以及 json 文件。应当与 {@link ConfigCategory#configEntries} 中的键保持完全一致。
   */
  public final String name;
  /**
   * 配置项的显示名称，可以带有格式或翻译。
   */
  public final Component displayName;
  /**
   * 配置项的描述。
   */
  public final @Nullable Component description;
  /**
   * 返回一个配置实例（不一定是当前配置实例）中代表该配置项的值的 {@link Function}。
   * <p>
   * 示例：{@code config -> config.ignoreBorder}
   */
  public final Function<C, T> getter;
  /**
   * 设置一个配置实例（不一定是当前配置实例）中代表该配置项的值的 {@link BiConsumer}。
   * <p>
   * 示例：{@code (config, value) -> config.ignoreBorder = value}
   */
  public final BiConsumer<C, T> setter;
  /**
   * 对配置项的值进行验证，当值不符合要求时，将抛出 {@link CommandSyntaxException}。如果任何值都符合要求，可使用 {@link #NO_OP_VALIDATOR}。
   */
  protected final FailableConsumer<? super T, CommandSyntaxException> valueValidator;
  /**
   * 该配置项的默认值。
   */
  public final T defaultValue;

  /**
   * @see #builder
   */
  private ConfigEntry(ConfigCategory<C> category, ConfigEntryType<T> type, String name, Component displayName, @Nullable Component description, Function<C, T> getter, BiConsumer<C, T> setter, FailableConsumer<? super T, CommandSyntaxException> valueValidator, T defaultValue) {
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

  /**
   * 获取该配置值的当前值。
   * <p>
   * 注意：在实例游戏中，可直接访问对应的字段，无需调用此方法。例如，可直接使用 <code>{@linkplain BlockOperationConfig#current}.{@linkplain BlockOperationConfig#maxHistoryCount maxHistoryCount}</code>。
   */
  public T getCurrent() {
    return getter.apply(category.getCurrent());
  }

  /**
   * 设置该配置的当前值。
   * <p>
   * 注意：在实例游戏中，可直接设置对应的字段，无需调用此方法。例如，可直接使用 {@code BlockOperationConfig.current.maxHistoryCount = 5}。
   */
  public void setCurrent(T value) throws CommandSyntaxException {
    valueValidator.accept(value);
    setter.accept(category.getCurrent(), value);
  }

  /**
   * 构建新的配置项，可调用各方法以设置该配置项的参数。调用 {@link Builder#build()} 以完成构建。
   */
  public static <C, T> Builder<C, T> builder(ConfigCategory<C> category, ConfigEntryType<T> type, String name) {
    return new Builder<>(category, type, name);
  }

  public static class Builder<C, T> {
    private final ConfigCategory<C> category;
    private final ConfigEntryType<T> type;
    private final String name;
    private final String displayNameTranslationKey;
    private final String descriptionTranslationKey;
    private @Nullable Component displayName;
    private @Nullable Component description;
    private @Nullable Function<C, T> getter;
    private @Nullable BiConsumer<C, T> setter;
    private FailableConsumer<? super T, CommandSyntaxException> valueValidator = NO_OP_VALIDATOR;
    private @Nullable T defaultValue;

    /**
     * 创建新的配置项的构建器，可调用各方法以设置该配置项的参数。调用 {@link Builder#build()} 以完成构建。
     */
    public Builder(ConfigCategory<C> category, ConfigEntryType<T> type, String name) {
      this.category = category;
      this.type = type;
      this.name = name;
      this.displayNameTranslationKey = "enhanced_commands.config." + category.name + "." + name;
      this.descriptionTranslationKey = "enhanced_commands.config." + category.name + "." + name + ".description";
    }

    /**
     * 设置配置项的显示名称。
     */
    public Builder<C, T> setDisplayName(Component displayName) {
      this.displayName = displayName;
      return this;
    }

    /**
     * 设置配置项的显示名称，将以根据 {@link #name} 自动生成的翻译键作为参数。
     *
     * @param translationKeyToText 以翻译键作为参数（可忽略）生成 {@link Component} 对象的函数，例如：{@code translationKey -> Text.translatable(translationKey, ...)}
     */
    public Builder<C, T> setDisplayName(Function<String, Component> translationKeyToText) {
      this.displayName = translationKeyToText.apply(displayNameTranslationKey);
      return this;
    }

    /**
     * 设置配置项的描述。
     */
    public Builder<C, T> setDescription(@Nullable Component description) {
      this.description = description;
      return this;
    }

    /**
     * 设置配置项的描述，将以根据 {@link #name} 自动生成的翻译键作为参数。
     *
     * @param translationKeyToText 以翻译键作为参数（可忽略）生成 {@link Component} 对象的函数，例如：{@code translationKey -> Text.translatable(translationKey, ...)}
     */
    public Builder<C, T> setDescription(Function<String, @Nullable Component> translationKeyToText) {
      this.description = translationKeyToText.apply(descriptionTranslationKey);
      return this;
    }

    /**
     * 在已有的描述的基础上增加一行描述。
     */
    public Builder<C, T> appendDescription(@Nullable Component description) {
      if (this.description == null) {
        this.description = description;
      } else if (description != null) {
        this.description = Component.empty()
            .append(this.description)
            .append(CommonComponents.NEW_LINE)
            .append(description);
      }
      return this;
    }

    public Builder<C, T> setGetter(Function<C, T> getter) {
      this.getter = getter;
      return this;
    }

    public Builder<C, T> setSetter(BiConsumer<C, T> setter) {
      this.setter = setter;
      return this;
    }

    public Builder<C, T> setValueValidator(FailableConsumer<? super T, CommandSyntaxException> valueValidator) {
      this.valueValidator = valueValidator;
      return this;
    }

    public Builder<C, T> setDefaultValue(T defaultValue) {
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

}
