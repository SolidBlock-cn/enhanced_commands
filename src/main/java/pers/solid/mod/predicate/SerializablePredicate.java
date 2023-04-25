package pers.solid.mod.predicate;

import net.minecraft.nbt.NbtElement;

public interface SerializablePredicate {
  String asString();

  default NbtElement asNbt() {
    // TODO: 2023/4/24, 024 check
    return null;
  }
}
