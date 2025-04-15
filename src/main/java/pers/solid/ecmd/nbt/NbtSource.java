package pers.solid.ecmd.nbt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * NBT 的来源。可以是方块、实体或者存储。
 *
 * @param <T> 包含 NBT 数据的对象，如方块实体、实体等。
 */
public interface NbtSource<T> {
  DynamicCommandExceptionType QUERY_SCALE_NOT_NUMBER = new DynamicCommandExceptionType((path) -> Text.translatable("enhanced_commands.nbt.query_scale_not_number", path.toString()));
  int QUERY_LIMIT = 12;

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

  NbtCompound getNbtFor(T source, RegistryWrapper.@NotNull WrapperLookup registryLookup);

  default Map<T, NbtCompound> getNbts(RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    return getNbts(FailableFunction.identity(), registryLookup);
  }

  default Map<T, NbtElement> getNbts(@Nullable NbtPathArgumentType.NbtPath nbtPath, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    return getNbts(nbtPath == null ? nbtCompound -> nbtCompound : nbtCompound -> Iterables.getOnlyElement(nbtPath.get(nbtCompound)), registryLookup);
  }

  default <R> Map<T, R> getNbts(FailableFunction<NbtCompound, R, CommandSyntaxException> mappingFunction, RegistryWrapper.@NotNull WrapperLookup registryLookup) throws CommandSyntaxException {
    final ImmutableMap.Builder<T, R> builder = new ImmutableMap.Builder<>();
    for (T value : values()) {
      builder.put(value, mappingFunction.apply(getNbtFor(value, registryLookup)));
    }
    return builder.build();
  }

  NbtElement concentrateNbts(Collection<? extends NbtElement> nbtElements) throws CommandSyntaxException;

  int executeQuery(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path, double scale) throws CommandSyntaxException;

  default NbtElement getConcentratedNbts(@Nullable NbtPathArgumentType.NbtPath path, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    final Map<T, NbtElement> nbts = getNbts(path, registryLookup);
    return concentrateNbts(nbts.values());
  }

  interface Single<T> extends NbtSource<T> {
    T value();

    @Override
    default Collection<T> values() {
      return Collections.singletonList(value());
    }

    default NbtCompound getNbt(RegistryWrapper.@NotNull WrapperLookup registryLookup) {
      return getNbtFor(value(), registryLookup);
    }

    @Override
    default <R> Map<T, R> getNbts(FailableFunction<NbtCompound, R, CommandSyntaxException> mappingFunction, RegistryWrapper.@NotNull WrapperLookup registryLookup) throws CommandSyntaxException {
      return Map.of(value(), mappingFunction.apply(getNbt(registryLookup)));
    }

    @Override
    default NbtElement concentrateNbts(Collection<? extends NbtElement> nbtElements) {
      return Iterables.getOnlyElement(nbtElements);
    }
  }
}
