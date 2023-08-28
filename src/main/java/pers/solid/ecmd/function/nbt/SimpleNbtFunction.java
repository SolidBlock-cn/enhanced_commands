package pers.solid.ecmd.function.nbt;

import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.TextUtil;

/**
 * 无论原先值，直接返回固定时的 NBT 函数。
 *
 * @param element 使用时需要返回的值。
 */
public record SimpleNbtFunction(@NotNull NbtElement element) implements NbtFunction {
  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (requirePrefix ? ": " : "") + TextUtil.toSpacedStringNbt(element);
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement) {
    return element;
  }
}
