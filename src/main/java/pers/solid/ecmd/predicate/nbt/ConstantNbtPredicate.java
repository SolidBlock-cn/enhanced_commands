package pers.solid.ecmd.predicate.nbt;

import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;

public enum ConstantNbtPredicate implements NbtPredicate {
  TRUE(true),
  FALSE(false);

  private final boolean value;

  ConstantNbtPredicate(boolean value) {
    this.value = value;
  }

  @Override
  public @NotNull String asString() {
    return asString(false);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (value ? "" : "!") + (requirePrefix ? ": " : "") + "*";
  }

  @Override
  public boolean test(@NotNull NbtElement nbtElement) {
    return value;
  }

  public static ConstantNbtPredicate of(boolean value) {
    return value ? TRUE : FALSE;
  }
}
