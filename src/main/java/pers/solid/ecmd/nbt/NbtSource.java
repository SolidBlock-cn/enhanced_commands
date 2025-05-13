package pers.solid.ecmd.nbt;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.NbtConcentrationType;

import java.util.*;

/**
 * NBT 的来源。可以是方块、实体或者存储。
 *
 * @param <T> 包含 NBT 数据的对象，如方块实体、实体等。
 */
public interface NbtSource<T> {
  DynamicCommandExceptionType QUERY_SCALE_NOT_NUMBER = new DynamicCommandExceptionType((path) -> Text.translatable("enhanced_commands.nbt.query_scale_not_number", path.toString()));
  int QUERY_LIMIT = 12;
  SimpleCommandExceptionType GET_MULTIPLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.data.get.multiple"));

  /**
   * 类似于原版的行为，返回指定的 nbt 数值在缩放后的值。如果 nbt 的值不是数字，则抛出错误。
   */
  static double scaleNbt(NbtElement nbtElement, double scale, NbtPathArgumentType.NbtPath path) throws CommandSyntaxException {
    if (nbtElement instanceof AbstractNbtNumber number) {
      return number.doubleValue() * scale;
    } else {
      throw QUERY_SCALE_NOT_NUMBER.create(path);
    }
  }

  /**
   * 类似于原版的行为，将 nbtElement 转换为数字，可以是其包含的元素的数量，作为命令的返回值。
   */
  static int toInt(NbtElement nbtElement) {
    switch (nbtElement) {
      case NbtInt nbtInt -> {
        return nbtInt.intValue();
      }
      case NbtLong nbtLong -> {
        return nbtLong.intValue();
      }
      case NbtShort nbtShort -> {
        return nbtShort.intValue();
      }
      case AbstractNbtNumber nbtNumber -> {
        return MathHelper.floor(nbtNumber.doubleValue());
      }
      case AbstractNbtList<?> nbtList -> {
        return nbtList.size();
      }
      case NbtCompound nbtCompound -> {
        return nbtCompound.getSize();
      }
      default -> {
        return 1;
      }
    }
  }

  Collection<T> values();

  NbtCompound getNbtFor(T source, @NotNull RegistryWrapper.WrapperLookup registryLookup);

  default NbtElement getNbtFor(T source, @Nullable NbtPathArgumentType.NbtPath path, @NotNull RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    final NbtCompound nbt = getNbtFor(source, registryLookup);
    if (path == null) {
      return nbt;
    }
    final List<NbtElement> nbtInPath = path.get(nbt);
    Iterator<NbtElement> iterator = nbtInPath.iterator();
    NbtElement nbtElement = iterator.next();
    if (iterator.hasNext()) {
      throw GET_MULTIPLE_EXCEPTION.create();
    } else {
      return nbtElement;
    }
  }

  default Map<T, NbtElement> getNbts(@Nullable NbtPathArgumentType.NbtPath path, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    final ImmutableMap.Builder<T, NbtElement> builder = new ImmutableMap.Builder<>();
    for (T value : values()) {
      try {
        builder.put(value, getNbtFor(value, path, registryLookup));
      } catch (CommandSyntaxException e) {
        // skip
      }
    }
    return builder.build();
  }

  int executeQuery(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path, double scale, NbtConcentrationType nbtConcentrationType, Random random) throws CommandSyntaxException;

  default NbtElement getConcentratedNbts(@Nullable NbtPathArgumentType.NbtPath path, RegistryWrapper.WrapperLookup registryLookup, NbtConcentrationType nbtConcentrationType, Random random) throws CommandSyntaxException {
    final Map<T, NbtElement> nbts = getNbts(path, registryLookup);
    return nbtConcentrationType.concentrate(nbts.values(), random);
  }

  interface Single<T> extends NbtSource<T> {
    T value();

    @Override
    default Collection<T> values() {
      return Collections.singletonList(value());
    }

    default NbtElement getNbt(NbtPathArgumentType.@Nullable NbtPath path, RegistryWrapper.@NotNull WrapperLookup registryLookup) throws CommandSyntaxException {
      return getNbtFor(value(), path, registryLookup);
    }

    @Override
    default Map<T, NbtElement> getNbts(NbtPathArgumentType.@Nullable NbtPath path, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
      return Map.of(value(), getNbt(path, registryLookup));
    }
  }
}
