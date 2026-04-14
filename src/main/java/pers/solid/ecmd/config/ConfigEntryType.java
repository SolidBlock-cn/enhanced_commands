package pers.solid.ecmd.config;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.StreamCodec;
import pers.solid.ecmd.util.TextUtil;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * <p>配置项的类型，该类型将影响着如何存储为 json、如何转化为数据包（packet）以在客户端和服务器之间通信，以及如何在命令中作为参数等。
 * <p>本模组使用的配置项类型存储于 {@link ConfigEntryTypes}。
 *
 * @param <T> 配置项的值的数据类型
 */
public interface ConfigEntryType<T> {
  /**
   * 从一个 {@link Class} 对象获取对应的 {@link ConfigEntryType}，用于反射。请注意：仅识别本模组已经创建好的几个类型，功能有限。
   *
   * @param classObject Java 的类对象
   * @return 对应类的 {@link ConfigEntryType} 对象，通常不是一个新的对象
   */
  @SuppressWarnings("unchecked")
  static <T> ConfigEntryType<T> fromClass(Class<T> classObject) {
    final ConfigEntryType<?> configEntryType = ConfigEntryTypes.CLASS_TO_TYPE.get(classObject);
    if (configEntryType == null) {
      throw new IllegalArgumentException("No such config entry type for " + classObject);
    } else {
      return (ConfigEntryType<T>) configEntryType;
    }
  }

  /**
   * 配置类型的 codec，主要用于序列化为 json 文件。
   */
  Codec<T> codec();

  /**
   * 配置类型的 packet codec，主要用于将该配置类型的值在客户端与服务器之间通信。
   */
  StreamCodec<? super RegistryFriendlyByteBuf, T> packetCodec();

  /**
   * 以 {@link Component} 的形式显示该配置类型的值。
   */
  Component displayValue(T value);

  /**
   * 以 {@link Component} 的形式显示该配置类型的值，并添加相应样式。不需要重写此方法。
   */
  default Component displayValue(T value, UnaryOperator<Style> styleUpdater) {
    return TextUtil.styled(displayValue(value), styleUpdater);
  }


  /**
   * 该配置类型对应的 {@link ArgumentType}，用于在命令中解析参数。
   */
  ArgumentType<T> getArgumentType(CommandBuildContext commandBuildContext);

  /**
   * 创建一个简单的 {@link ConfigEntryType}，其中 {@link ArgumentType} 为恒定值，不受 {@code commandBuildContext} 的影响。
   */
  static <T> ConfigEntryType<T> of(Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> packetCodec, Function<T, Component> displayFunction, ArgumentType<T> argumentType) {
    return new Simple<>(codec, packetCodec, displayFunction, (n) -> argumentType);
  }

  /**
   * 创建一个简单的 {@link ConfigEntryType}。
   */
  static <T> ConfigEntryType<T> of(Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> packetCodec, Function<T, Component> displayFunction, Function<CommandBuildContext, ArgumentType<T>> argumentTypeProvider) {
    return new Simple<>(codec, packetCodec, displayFunction, argumentTypeProvider);
  }

  record Simple<T>(Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> packetCodec, Function<T, Component> displayFunction, Function<CommandBuildContext, ? extends ArgumentType<T>> argumentTypeProvider) implements ConfigEntryType<T> {
    @Override
    public Component displayValue(T value) {
      return displayFunction.apply(value);
    }

    @Override
    public ArgumentType<T> getArgumentType(CommandBuildContext commandBuildContext) {
      return argumentTypeProvider.apply(commandBuildContext);
    }
  }
}
