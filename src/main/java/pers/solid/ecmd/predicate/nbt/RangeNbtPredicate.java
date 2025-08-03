package pers.solid.ecmd.predicate.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.bridge.BridgeDoubleRange;
import pers.solid.ecmd.util.bridge.BridgeFloatRange;
import pers.solid.ecmd.util.bridge.BridgeIntRange;
import pers.solid.ecmd.util.bridge.BridgeRange;

public record RangeNbtPredicate(BridgeRange<?> numberRange, boolean inverted) implements NbtPredicate {
  public static final MapCodec<RangeNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BridgeRange.CODEC.forGetter(RangeNbtPredicate::numberRange), Codec.BOOL.optionalFieldOf("inverted", false).forGetter(RangeNbtPredicate::inverted)).apply(i, RangeNbtPredicate::new));

  @Override
  public @NotNull String asString() {
    return asString(false);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (inverted ? "!" : "") + (requirePrefix ? ": " : "") + numberRange.asString();
  }

  @Override
  public boolean test(@NotNull NbtElement nbtElement) {
    if (!(nbtElement instanceof final AbstractNbtNumber nbtNumber))
      return inverted;
    if (numberRange instanceof BridgeDoubleRange doubleRange) {
      return doubleRange.test(nbtNumber.doubleValue()) != inverted;
    } else if (numberRange instanceof BridgeFloatRange floatRange) {
      return floatRange.test(nbtNumber.floatValue()) != inverted;
    } else if (numberRange instanceof BridgeIntRange intRange) {
      return intRange.test(nbtNumber.intValue()) != inverted;
    } else {
      return inverted;
    }
  }

  @Override
  public @NotNull NbtPredicateType<RangeNbtPredicate> getType() {
    return Type.RANGE_TYPE;
  }

  public enum Type implements NbtPredicateType<RangeNbtPredicate> {
    RANGE_TYPE;

    @Override
    public MapCodec<RangeNbtPredicate> getCodec() {
      return CODEC;
    }
  }
}
