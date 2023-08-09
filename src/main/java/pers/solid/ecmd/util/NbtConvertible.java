package pers.solid.ecmd.util;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Contract;

/**
 * 此接口用于与 NBT 对象能够相互转换的对象。
 */
public interface NbtConvertible {
  void writeNbt(NbtCompound nbtCompound);

  @Contract(pure = true)
  default NbtCompound createNbt() {
    final NbtCompound nbt = new NbtCompound();
    writeNbt(nbt);
    return nbt;
  }
}
