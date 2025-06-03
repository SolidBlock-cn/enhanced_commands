package pers.solid.ecmd.nbt;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.NbtConcentrationType;

import java.util.*;

/**
 * NBT 的来源。可以是方块、实体或者存储。
 *
 * @param <T> 包含 NBT 数据的对象，如方块实体、实体等。
 */
public interface NbtSource<T> {
  DynamicCommandExceptionType QUERY_SCALE_NOT_NUMBER = new DynamicCommandExceptionType((path) -> Text.translatable("enhanced_commands.commands.nbt.query_scale_not_number", path.toString()));
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
    if (Objects.requireNonNull(nbtElement) instanceof NbtInt nbtInt) {
      return nbtInt.intValue();
    } else if (nbtElement instanceof NbtLong nbtLong) {
      return nbtLong.intValue();
    } else if (nbtElement instanceof NbtShort nbtShort) {
      return nbtShort.intValue();
    } else if (nbtElement instanceof AbstractNbtNumber nbtNumber) {
      return MathHelper.floor(nbtNumber.doubleValue());
    } else if (nbtElement instanceof AbstractNbtList<?> nbtList) {
      return nbtList.size();
    } else if (nbtElement instanceof NbtCompound nbtCompound) {
      return nbtCompound.getSize();
    }
    return 1;
  }

  Collection<T> values(ServerCommandSource source);

  NbtCompound getNbtFor(ServerCommandSource commandSource, T source);

  default NbtElement getNbtInPathFor(ServerCommandSource commandSource, T source, @Nullable NbtPathArgumentType.NbtPath path) throws CommandSyntaxException {
    final NbtCompound nbt = getNbtFor(commandSource, source);
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

  default Map<T, NbtElement> getNbtsInPath(ServerCommandSource source, @Nullable NbtPathArgumentType.NbtPath path) throws CommandSyntaxException {
    final ImmutableMap.Builder<T, NbtElement> builder = new ImmutableMap.Builder<>();
    for (T value : values(source)) {
      try {
        builder.put(value, getNbtInPathFor(source, value, path));
      } catch (CommandSyntaxException e) {
        // skip
      }
    }
    return builder.build();
  }

  int executeQuery(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path, double scale, NbtConcentrationType nbtConcentrationType, Random random) throws CommandSyntaxException;

  default NbtElement getConcentratedNbts(ServerCommandSource commandSource, @Nullable NbtPathArgumentType.NbtPath path, NbtConcentrationType nbtConcentrationType, Random random) throws CommandSyntaxException {
    final Map<T, NbtElement> nbts = getNbtsInPath(commandSource, path);
    return nbtConcentrationType.concentrate(nbts.values(), random);
  }

  /**
   * 表示单个的 NBT 来源，其一些方法可以有所优化。
   */
  interface Single<T> extends NbtSource<T> {
    T value(ServerCommandSource commandSource);

    @Override
    default Collection<T> values(ServerCommandSource source) {
      return Collections.singletonList(value(source));
    }

    default NbtElement getNbtInPath(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path) throws CommandSyntaxException {
      return getNbtInPathFor(source, value(source), path);
    }

    @Override
    default Map<T, NbtElement> getNbtsInPath(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path) throws CommandSyntaxException {
      return Map.of(value(source), getNbtInPath(source, path));
    }
  }
}
