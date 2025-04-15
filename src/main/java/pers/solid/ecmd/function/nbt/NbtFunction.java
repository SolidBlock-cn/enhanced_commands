package pers.solid.ecmd.function.nbt;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.Set;

public interface NbtFunction extends ExpressionConvertible {
  @Override
  @NotNull
  default String asString() {
    return asString(false);
  }

  @NotNull String asString(boolean requirePrefix);

  /**
   * 根据现有的 NBT 元素（可能为 null）返回所需要的 NBT 元素。原先的 NBT 元素可能会被完全忽略。当接收的 NBT 元素为可变对象时，可能会直接修改并返回它。
   */
  @NotNull NbtElement apply(@Nullable NbtElement nbtElement);

  default @NotNull NbtElement recursivelyApply(NbtElement nbtElement, NbtPredicate predicate) {
    switch (nbtElement) {
      case NbtCompound nbtCompound -> {
        if (predicate.test(nbtCompound)) {
          return apply(nbtElement);
        } else {
          final Set<String> keys = nbtCompound.getKeys();
          for (String key : keys) {
            final NbtElement value = nbtCompound.get(key);
            if (value == null) continue;
            final NbtElement applied = recursivelyApply(value, predicate);
            if (applied != value) {
              nbtCompound.put(key, applied);
            }
          }
          return nbtCompound;
        }
      }
      case NbtList nbtList -> {
        if (predicate.test(nbtList)) {
          return apply(nbtElement);
        } else {
          for (int i = 0; i < nbtList.size(); i++) {
            final NbtElement value = nbtList.get(i);
            final NbtElement applied = recursivelyApply(value, predicate);
            if (applied != value) {
              nbtList.setElement(i, applied);
            }
          }
          return nbtList;
        }
      }
      default -> {
        if (predicate.test(nbtElement)) {
          return apply(nbtElement);
        } else {
          return nbtElement;
        }
      }
    }
  }
}
