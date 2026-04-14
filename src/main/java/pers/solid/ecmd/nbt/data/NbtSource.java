package pers.solid.ecmd.nbt.data;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.*;

/**
 * NBT 的来源。可以是方块、实体或者存储。
 *
 * @param <T> 包含 NBT 数据的对象，如方块实体、实体等。
 */
public interface NbtSource<T> extends ExpressionConvertible {
  Codec<NbtSource<?>> CODEC = Type.CODEC.dispatch(NbtSource::getType, Type::getCodec);

  DynamicCommandExceptionType QUERY_SCALE_NOT_NUMBER = new DynamicCommandExceptionType((path) -> Component.translatable("enhanced_commands.commands.nbt.query_scale_not_number", path.toString()));
  int QUERY_LIMIT = 12;
  SimpleCommandExceptionType GET_MULTIPLE_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("commands.data.get.multiple"));

  /**
   * 类似于原版的行为，返回指定的 nbt 数值在缩放后的值。如果 nbt 的值不是数字，则抛出错误。
   */
  static double scaleNbt(Tag nbtElement, double scale, NbtPathArgument.NbtPath path) throws CommandSyntaxException {
    if (nbtElement instanceof NumericTag number) {
      return number.getAsDouble() * scale;
    } else {
      throw QUERY_SCALE_NOT_NUMBER.create(path);
    }
  }

  /**
   * 类似于原版的行为，将 nbtElement 转换为数字，可以是其包含的元素的数量，作为命令的返回值。
   */
  static int toInt(Tag nbtElement) {
    if (Objects.requireNonNull(nbtElement) instanceof IntTag nbtInt) {
      return nbtInt.getAsInt();
    } else if (nbtElement instanceof LongTag nbtLong) {
      return nbtLong.getAsInt();
    } else if (nbtElement instanceof ShortTag nbtShort) {
      return nbtShort.getAsInt();
    } else if (nbtElement instanceof NumericTag nbtNumber) {
      return Mth.floor(nbtNumber.getAsDouble());
    } else if (nbtElement instanceof CollectionTag<?> nbtList) {
      return nbtList.size();
    } else if (nbtElement instanceof CompoundTag nbtCompound) {
      return nbtCompound.size();
    }
    return 1;
  }

  Collection<T> values(CommandSourceStack source) throws CommandSyntaxException;

  CompoundTag getNbtFor(CommandSourceStack commandSource, T source);

  Type getType();

  default Tag getNbtInPathFor(CommandSourceStack commandSource, T source, @Nullable NbtPathArgument.NbtPath path) throws CommandSyntaxException {
    final CompoundTag nbt = getNbtFor(commandSource, source);
    if (path == null) {
      return nbt;
    }
    final List<Tag> nbtInPath = path.get(nbt);
    Iterator<Tag> iterator = nbtInPath.iterator();
    Tag nbtElement = iterator.next();
    if (iterator.hasNext()) {
      throw GET_MULTIPLE_EXCEPTION.create();
    } else {
      return nbtElement;
    }
  }

  default Map<T, Tag> getNbtsInPath(CommandSourceStack source, @Nullable NbtPathArgument.NbtPath path) throws CommandSyntaxException {
    final ImmutableMap.Builder<T, Tag> builder = new ImmutableMap.Builder<>();
    for (T value : values(source)) {
      try {
        builder.put(value, getNbtInPathFor(source, value, path));
      } catch (CommandSyntaxException e) {
        // skip
      }
    }
    return builder.build();
  }

  int executeQuery(CommandSourceStack source, NbtPathArgument.@Nullable NbtPath path, double scale, NbtConcentrationType nbtConcentrationType, RandomSource random) throws CommandSyntaxException;

  default Tag getConcentratedNbts(CommandSourceStack commandSource, @Nullable NbtPathArgument.NbtPath path, NbtConcentrationType nbtConcentrationType, RandomSource random) throws CommandSyntaxException {
    final Map<T, Tag> nbts = getNbtsInPath(commandSource, path);
    return nbtConcentrationType.concentrate(nbts.values(), random);
  }

  /**
   * 表示单个的 NBT 来源，其一些方法可以有所优化。
   */
  interface Single<T> extends NbtSource<T> {
    T value(CommandSourceStack commandSource) throws CommandSyntaxException;

    @Override
    default Collection<T> values(CommandSourceStack source) throws CommandSyntaxException {
      return Collections.singletonList(value(source));
    }

    default Tag getNbtInPath(CommandSourceStack source, NbtPathArgument.@Nullable NbtPath path) throws CommandSyntaxException {
      return getNbtInPathFor(source, value(source), path);
    }

    @Override
    default Map<T, Tag> getNbtsInPath(CommandSourceStack source, NbtPathArgument.@Nullable NbtPath path) throws CommandSyntaxException {
      return Map.of(value(source), getNbtInPath(source, path));
    }
  }

  enum Type implements StringRepresentable {
    BLOCK("block", BlockNbtData.CODEC),
    ENTITY("entity", EntityNbtData.CODEC),
    LITERAL("literal", LiteralNbtData.CODEC),
    STORAGE("storage", StorageNbtData.CODEC);
    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(values());

    private final String name;
    private final MapCodec<? extends NbtTarget<?>> codec;

    Type(String name, MapCodec<? extends NbtTarget<?>> codec) {
      this.name = name;
      this.codec = codec;
    }

    public MapCodec<? extends NbtTarget<?>> getCodec() {
      return codec;
    }

    @Override
    public String getSerializedName() {
      return name;
    }
  }
}
