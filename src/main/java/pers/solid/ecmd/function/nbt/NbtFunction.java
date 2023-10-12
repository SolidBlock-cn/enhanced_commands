package pers.solid.ecmd.function.nbt;

import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExpressionConvertible;

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
}
