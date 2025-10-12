package pers.solid.ecmd.configs;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.TextUtil;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface ConfigEntryType<T> {
  @NotNull Codec<T> codec();

  @NotNull PacketCodec<? super RegistryByteBuf, T> packetCodec();

  @NotNull Text displayValue(T value);

  @NotNull
  default Text displayValue(T value, UnaryOperator<Style> styleUpdater) {
    return TextUtil.styled(displayValue(value), styleUpdater);
  }


  @NotNull ArgumentType<T> getArgumentType(CommandRegistryAccess registryAccess);

  static <T> ConfigEntryType<T> of(Codec<T> codec, PacketCodec<? super RegistryByteBuf, T> packetCodec, Function<T, Text> displayFunction, ArgumentType<T> argumentType) {
    return new Simple<>(codec, packetCodec, displayFunction, (n) -> argumentType);
  }

  static <T> ConfigEntryType<T> of(Codec<T> codec, PacketCodec<? super RegistryByteBuf, T> packetCodec, Function<T, Text> displayFunction, Function<CommandRegistryAccess, ArgumentType<T>> argumentTypeProvider) {
    return new Simple<>(codec, packetCodec, displayFunction, argumentTypeProvider);
  }

  record Simple<T>(Codec<T> codec, PacketCodec<? super RegistryByteBuf, T> packetCodec, Function<T, Text> displayFunction, Function<CommandRegistryAccess, ArgumentType<T>> argumentTypeProvider) implements ConfigEntryType<T> {
    @Override
    public @NotNull Text displayValue(T value) {
      return displayFunction.apply(value);
    }

    @Override
    public @NotNull ArgumentType<T> getArgumentType(CommandRegistryAccess registryAccess) {
      return argumentTypeProvider.apply(registryAccess);
    }
  }
}
