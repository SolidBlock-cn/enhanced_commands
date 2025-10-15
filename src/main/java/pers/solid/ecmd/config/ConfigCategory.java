package pers.solid.ecmd.config;

import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.*;

/**
 * <p>配置项的分类。
 * <p>本模组的配置系统分为多个分类，每个分类都可以有一些配置项。不同的分类的配置存储在不同的 json 文件中。在本模组中，各配置项是通过反射来实现的，参见 {@link ConfigReflectionHelper}。本模组使用的各配置分类存储于 {@link ConfigCategories}。
 *
 * @param <C> 代表一个配置实例的类，该类的各实例字段就是配置项，其构造函数通常是默认的。
 * @see ConfigReflectionHelper
 * @see ConfigCategories
 */
public class ConfigCategory<C> {
  /**
   * 本模组的各配置分类的注册表。
   */
  public static final Map<String, ConfigCategory<?>> REGISTRY = new LinkedHashMap<>();
  /**
   * 配置分类的名称，将影响命令的参数以及存储的 json 文件的名字。在写入注册表时，应当与在注册表中的键保持一致。
   */
  public final @NotNull String name;
  /**
   * 配置分类在游戏内的显示名称，可以带有格式或翻译等。
   */
  public final @NotNull Text displayName;
  /**
   * 配置分类的描述。
   */
  public final @Nullable Text description;
  /**
   * 代表该分类的默认配置的实例。这一实例通常不应当被修改。
   */
  public final C defaultConfig;
  /**
   * 返回代表当前配置分类的配置实例的 {@link Supplier}。在本模组中，一个配置分类的当前配置的实例时该类名为 {@code current} 的静态字段。
   * <p>
   * 示例：{@code () -> BlockOperationConfig.current}
   */
  public final Supplier<C> currentConfigGetter;
  /**
   * 设置代表当前配置分类的配置实例的 {@link Consumer}。
   * <p>
   * 示例：{@code c -> BlockOperationConfig.current = s}
   */
  public final Consumer<C> currentConfigSetter;
  /**
   * 当前配置分类项的各个配置项。
   */
  public final Map<String, ConfigEntry<C, ?>> configEntries = new LinkedHashMap<>();
  /**
   * 如果为 {@code true}，表明当前配置分类有配置项被改变了，无论是以何种方式改变。因此，在保存时会将所有 {@code dirt = true} 的配置分类进行保存。
   *
   * @see ConfigManager#saveCategoryToFile(ConfigCategory)
   */
  protected boolean dirty = false;

  /**
   * 创建一个新的配置分类，其所有参数都需要手动指定。
   *
   * @see #create
   */
  public ConfigCategory(@NotNull String name, @NotNull Text displayName, @Nullable Text description, C defaultConfig, Supplier<C> currentConfigGetter, Consumer<C> currentConfigSetter) {
    this.name = name;
    this.displayName = displayName;
    this.description = description;
    this.defaultConfig = defaultConfig;
    this.currentConfigGetter = currentConfigGetter;
    this.currentConfigSetter = currentConfigSetter;
  }

  /**
   * 创建一个新的配置分类，其显示名称和描述都将根据 {@code name} 来决定。
   */
  public static <C> ConfigCategory<C> create(@NotNull String name, C defaultConfig, Supplier<C> currentConfigGetter, Consumer<C> currentConfigSetter, boolean hasDescription) {
    return new ConfigCategory<>(name, Text.translatable("enhanced_commands.config." + name), hasDescription ? Text.translatable("enhanced_commands.config." + name + ".description") : null, defaultConfig, currentConfigGetter, currentConfigSetter);
  }

  /**
   * 创建属于该分类的一个配置项。
   */
  public <T> ConfigEntry<C, T> createEntry(@NotNull String name, @NotNull ConfigEntryType<T> type, @NotNull Function<C, T> getter, @NotNull BiConsumer<C, T> setter, @NotNull T defaultValue, @Nullable EntryModifier<C, T> modifier, boolean hasDescription) {
    ConfigEntry.Builder<C, T> builder = ConfigEntry.builder(this, type, name)
        .setGetter(getter)
        .setSetter(setter)
        .setDefaultValue(defaultValue)
        .setDisplayName(Text::translatable);
    if (hasDescription) {
      builder.setDescription(Text::translatable);
    }
    if (modifier != null) {
      builder = modifier.apply(builder);
    }
    return builder.build();
  }

  /**
   * 获取该配置分类当前的配置实例。
   * <p>
   * 注意，在实际游戏内容中，可直接访问代表该实例的字段。例如，可直接使用 {@link BlockOperationConfig#current} 而非 <code>{@linkplain ConfigCategories#BLOCK_OPERATION}.getCurrent()</code>。
   */
  public C getCurrent() {
    return currentConfigGetter.get();
  }

  /**
   * 将此类别的配置标记为 dirty，表示离开服务器时需要保存。
   */
  public void markDirty() {
    this.dirty = true;
  }

  /**
   * 如果为 true，表明该配置分类发生了更改，需要保存。
   */
  public boolean isDirty() {
    return this.dirty;
  }

  /**
   * 用于在构建配置项时对配置项进行修改的函数式接口。
   *
   * @see #createEntry
   */
  public interface EntryModifier<C, T> extends UnaryOperator<ConfigEntry.Builder<C, T>> {}
}
