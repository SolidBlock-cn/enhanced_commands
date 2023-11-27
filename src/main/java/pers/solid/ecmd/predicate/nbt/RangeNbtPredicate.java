package pers.solid.ecmd.predicate.nbt;

import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtElement;
import net.minecraft.predicate.NumberRange;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StringUtil;

public record RangeNbtPredicate(NumberRange<?> numberRange, boolean negated) implements NbtPredicate {
  @Override
  public @NotNull String asString() {
    return asString(false);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (negated ? "!" : "") + (requirePrefix ? ": " : "") + StringUtil.wrapRange(numberRange);
  }

  @Override
  public boolean test(@NotNull NbtElement nbtElement) {
    if (!(nbtElement instanceof final AbstractNbtNumber nbtNumber))
      return negated;
    if (numberRange instanceof NumberRange.FloatRange floatRange) {
      return floatRange.test(nbtNumber.doubleValue()) != negated;
    } else if (numberRange instanceof NumberRange.IntRange intRange) {
      return intRange.test(nbtNumber.intValue()) != negated;
    } else {
      return negated;
    }
  }
}
