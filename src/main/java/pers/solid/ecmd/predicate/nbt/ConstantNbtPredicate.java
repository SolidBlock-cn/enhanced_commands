package pers.solid.ecmd.predicate.nbt;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;

public enum ConstantNbtPredicate implements NbtPredicate {
  TRUE(true),
  FALSE(false);

  public static final Codec<ConstantNbtPredicate> CODEC = Codec.BOOL.xmap(b -> b ? TRUE : FALSE, v -> v == TRUE);
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

  @Override
  public @NotNull Type getType() {
    return Type.CONSTANT;
  }

  public static ConstantNbtPredicate of(boolean value) {
    return value ? TRUE : FALSE;
  }
}
