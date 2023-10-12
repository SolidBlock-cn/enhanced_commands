package pers.solid.ecmd.predicate.nbt;

import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExpressionConvertible;

public interface NbtPredicate extends ExpressionConvertible {
  @Override
  @NotNull String asString();

  default @NotNull String asString(boolean requirePrefix) {
    return asString();
  }

  boolean test(@NotNull NbtElement nbtElement);
}
