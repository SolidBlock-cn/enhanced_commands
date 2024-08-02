package pers.solid.ecmd.predicate.nbt;

import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.bridge.BridgeDoubleRange;
import pers.solid.ecmd.util.bridge.BridgeFloatRange;
import pers.solid.ecmd.util.bridge.BridgeIntRange;
import pers.solid.ecmd.util.bridge.BridgeRange;

public record RangeNbtPredicate(BridgeRange<?> numberRange, boolean negated) implements NbtPredicate {
  @Override
  public @NotNull String asString() {
    return asString(false);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (negated ? "!" : "") + (requirePrefix ? ": " : "") + numberRange.asString();
  }

  @Override
  public boolean test(@NotNull NbtElement nbtElement) {
    if (!(nbtElement instanceof final AbstractNbtNumber nbtNumber))
      return negated;
    if (numberRange instanceof BridgeDoubleRange doubleRange) {
      return doubleRange.test(nbtNumber.doubleValue()) != negated;
    } else if (numberRange instanceof BridgeFloatRange floatRange) {
      return floatRange.test(nbtNumber.floatValue()) != negated;
    } else if (numberRange instanceof BridgeIntRange intRange) {
      return intRange.test(nbtNumber.intValue()) != negated;
    } else {
      return negated;
    }
  }

  @Override
  public @NotNull Type getType() {
    return Type.RANGE;
  }
}
