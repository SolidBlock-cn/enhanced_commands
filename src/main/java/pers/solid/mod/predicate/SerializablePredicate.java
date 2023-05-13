package pers.solid.mod.predicate;

import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;

public interface SerializablePredicate {
  @NotNull String asString();

  default NbtElement asNbt() {
    // TODO: 2023/4/24, 024 nbt
    return null;
  }
}
