package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * <p>NBT 目标指定了将 NBT 数据存储在什么位置。自身是不带路径和聚合类型的，实际运行命令时通常需要搭配表示路径以及聚合类型的参数使用。
 * <p>这里为了方便，{@link NbtTarget} 直接继承了 {@link NbtSource}，这是考虑到所有的 NBT 目标首先都会是 NBT 来源。
 *
 * @param <T> NBT 目标所含具体对象的类型，可以是方块、实体等。
 */
public interface NbtTarget<T> extends NbtSource<T> {
  Codec<NbtTarget<?>> CODEC = Codec.lazyInitialized(() -> Type.CODEC).dispatch(NbtTarget::getType, Type::getCodec);

  /**
   * @return 所有可修改 NBT 数据的对象的集合。
   */
  @Override
  Collection<T> values(ServerCommandSource source) throws CommandSyntaxException;

  /**
   * 设置指定的对象的 NBT 数据。
   */
  void setNbtFor(ServerCommandSource commandSource, T target, NbtCompound nbt) throws CommandSyntaxException;

  default void setNbt(ServerCommandSource source, NbtCompound nbt) throws CommandSyntaxException {
    for (T value : values(source)) {
      setNbtFor(source, value, nbt);
    }
  }

  default void transformNbtFor(ServerCommandSource commandSource, T target, FailableFunction<NbtCompound, @Nullable NbtCompound, CommandSyntaxException> operation) throws CommandSyntaxException {
    setNbtFor(commandSource, target, operation.apply(getNbtFor(commandSource, target)));
  }

  default void transformNbt(ServerCommandSource commandSource, FailableFunction<NbtCompound, @Nullable NbtCompound, CommandSyntaxException> operator) throws CommandSyntaxException {
    for (T value : values(commandSource)) {
      transformNbtFor(commandSource, value, operator);
    }
  }

  default void transformNbtInPathFor(ServerCommandSource commandSource, T target, NbtPathArgumentType.NbtPath nbtPath, FailableFunction<NbtElement, @Nullable NbtElement, CommandSyntaxException> operator) throws CommandSyntaxException {
    final NbtElement original = getNbtInPathFor(commandSource, target, nbtPath);
    final NbtElement applied = operator.apply(original);
    setNbtInPathFor(commandSource, target, nbtPath, applied == null ? original : applied);
  }

  default void transformNbtInPath(ServerCommandSource commandSource, NbtPathArgumentType.NbtPath nbtPath, FailableFunction<NbtElement, @Nullable NbtElement, CommandSyntaxException> operator) throws CommandSyntaxException {
    for (T value : values(commandSource)) {
      transformNbtInPathFor(commandSource, value, nbtPath, operator);
    }
  }

  default void modifyNbtFor(ServerCommandSource commandSource, T target, FailableConsumer<NbtCompound, CommandSyntaxException> consumer) throws CommandSyntaxException {
    transformNbtFor(commandSource, target, nbtCompound -> {
      consumer.accept(nbtCompound);
      return nbtCompound;
    });
  }


  default void modifyNbt(ServerCommandSource commandSource, FailableConsumer<NbtCompound, CommandSyntaxException> consumer) throws CommandSyntaxException {
    for (T value : values(commandSource)) {
      modifyNbtFor(commandSource, value, consumer);
    }
  }

  default void setNbtInPathFor(ServerCommandSource commandSource, T target, NbtPathArgumentType.NbtPath nbtPath, NbtElement element) throws CommandSyntaxException {
    modifyNbtFor(commandSource, target, nbt -> {
      nbtPath.put(nbt, element);

      // 这样做是为了确保对根目录的 NBT 的修改也能正常生效。
      // 可能存在一点问题。
      if (element instanceof NbtCompound sourceCompound) {
        for (NbtElement nbtElement : nbtPath.get(nbt)) {
          if (nbtElement instanceof NbtCompound nbtCompound) {
            nbtCompound.copyFrom(sourceCompound);
          }
        }
      }
    });
  }

  default void setNbtInPath(ServerCommandSource commandSource, NbtPathArgumentType.NbtPath nbtPath, NbtElement element) throws CommandSyntaxException {
    for (T value : values(commandSource)) {
      setNbtInPathFor(commandSource, value, nbtPath, element);
    }
  }

  Text feedbackModify(Collection<T> values);

  /**
   * 单个的 NBT 目标。其方法在实现上会有些优化，以减少一些集合创建和迭代。
   */
  interface Single<T> extends NbtTarget<T>, NbtSource.Single<T> {
    T value(ServerCommandSource commandSource) throws CommandSyntaxException;

    @Override
    default Collection<T> values(ServerCommandSource source) throws CommandSyntaxException {
      return Collections.singleton(value(source));
    }

    @Override
    default void setNbt(ServerCommandSource source, NbtCompound nbt) throws CommandSyntaxException {
      setNbtFor(source, value(source), nbt);
    }

    @Override
    default void transformNbt(ServerCommandSource commandSource, FailableFunction<NbtCompound, NbtCompound, CommandSyntaxException> operator) throws CommandSyntaxException {
      transformNbtFor(commandSource, value(commandSource), operator);
    }

    @Override
    default void modifyNbt(ServerCommandSource commandSource, FailableConsumer<NbtCompound, CommandSyntaxException> consumer) throws CommandSyntaxException {
      modifyNbtFor(commandSource, value(commandSource), consumer);
    }

    @Override
    default void setNbtInPath(ServerCommandSource commandSource, NbtPathArgumentType.NbtPath nbtPath, NbtElement element) throws CommandSyntaxException {
      setNbtInPathFor(commandSource, value(commandSource), nbtPath, element);
    }
  }
}
