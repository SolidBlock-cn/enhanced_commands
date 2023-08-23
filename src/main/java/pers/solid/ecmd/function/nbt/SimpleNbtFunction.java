package pers.solid.ecmd.function.nbt;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.visitor.StringNbtWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 无论原先值，直接返回固定时的 NBT 函数。
 *
 * @param element 使用时需要返回的值。
 */
public record SimpleNbtFunction(@NotNull NbtElement element) implements NbtFunction {
  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (requirePrefix ? ": " : "") + new StringNbtWriter().apply(element);
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement) {
    return element;
  }
}
