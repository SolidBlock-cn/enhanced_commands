package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public interface NbtTarget<T> extends NbtSource<T> {
  Collection<T> values();

  void setNbtFor(T target, NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException;

  default void setNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    for (T value : values()) {
      setNbtFor(value, nbt, registryLookup);
    }
  }

  default void transformNbtFor(T target, FailableFunction<NbtCompound, @Nullable NbtCompound, CommandSyntaxException> operation, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    setNbtFor(target, operation.apply(getNbtFor(target, registryLookup)), registryLookup);
  }

  default void transformNbt(FailableFunction<NbtCompound, @Nullable NbtCompound, CommandSyntaxException> operator, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    for (T value : values()) {
      transformNbtFor(value, operator, registryLookup);
    }
  }

  default void transformNbtInPathFor(T target, NbtPathArgumentType.NbtPath nbtPath, FailableFunction<NbtElement, @Nullable NbtElement, CommandSyntaxException> operator, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    final NbtElement original = getNbtInPathFor(target, nbtPath, registryLookup);
    final NbtElement applied = operator.apply(original);
    setNbtInPathFor(target, nbtPath, applied == null ? original : applied, registryLookup);
  }

  default void transformNbtInPath(NbtPathArgumentType.NbtPath nbtPath, FailableFunction<NbtElement, @Nullable NbtElement, CommandSyntaxException> operator, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    for (T value : values()) {
      transformNbtInPathFor(value, nbtPath, operator, registryLookup);
    }
  }

  default void modifyNbtFor(T target, FailableConsumer<NbtCompound, CommandSyntaxException> consumer, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    transformNbtFor(target, nbtCompound -> {
      consumer.accept(nbtCompound);
      return nbtCompound;
    }, registryLookup);
  }


  default void modifyNbt(FailableConsumer<NbtCompound, CommandSyntaxException> consumer, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    for (T value : values()) {
      modifyNbtFor(value, consumer, registryLookup);
    }
  }

  default void setNbtInPathFor(T target, NbtPathArgumentType.NbtPath nbtPath, NbtElement element, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    modifyNbtFor(target, nbt -> {
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
    }, registryLookup);
  }

  default void setNbtInPath(NbtPathArgumentType.NbtPath nbtPath, NbtElement element, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    for (T value : values()) {
      setNbtInPathFor(value, nbtPath, element, registryLookup);
    }
  }

  Text feedbackModify();

  interface Single<T> extends NbtTarget<T>, NbtSource.Single<T> {
    T value();

    @Override
    default Collection<T> values() {
      return Collections.singleton(value());
    }

    @Override
    default void setNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
      setNbtFor(value(), nbt, registryLookup);
    }

    @Override
    default void transformNbt(FailableFunction<NbtCompound, NbtCompound, CommandSyntaxException> operator, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
      transformNbtFor(value(), operator, registryLookup);
    }

    @Override
    default void modifyNbt(FailableConsumer<NbtCompound, CommandSyntaxException> consumer, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
      modifyNbtFor(value(), consumer, registryLookup);
    }

    @Override
    default void setNbtInPath(NbtPathArgumentType.NbtPath nbtPath, NbtElement element, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
      setNbtInPathFor(value(), nbtPath, element, registryLookup);
    }
  }
}
