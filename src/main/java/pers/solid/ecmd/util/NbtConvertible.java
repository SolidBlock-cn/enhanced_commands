package pers.solid.ecmd.util;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * 此接口用于与 NBT 对象能够相互转换的对象。
 */
public interface NbtConvertible {
  @Contract(mutates = "param1")
  void writeNbt(@NotNull NbtCompound nbtCompound);

  @Contract(mutates = "param1")
  default void writeIdentifyingData(@NotNull NbtCompound nbtCompound) {
  }

  @Contract(pure = true)
  default NbtCompound createNbt() {
    final NbtCompound nbt = new NbtCompound();
    writeIdentifyingData(nbt);
    writeNbt(nbt);
    return nbt;
  }
}
