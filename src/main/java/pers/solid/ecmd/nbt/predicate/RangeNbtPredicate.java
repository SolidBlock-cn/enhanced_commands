package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import pers.solid.ecmd.util.bridge.BridgeDoubleRange;
import pers.solid.ecmd.util.bridge.BridgeFloatRange;
import pers.solid.ecmd.util.bridge.BridgeIntRange;
import pers.solid.ecmd.util.bridge.BridgeRange;

public record RangeNbtPredicate(BridgeRange<?> numberRange) implements NbtPredicate {
  public static final MapCodec<RangeNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BridgeRange.CODEC.forGetter(RangeNbtPredicate::numberRange)).apply(i, RangeNbtPredicate::new));

  @Override
  public String expressAsString() {
    return asString(false);
  }

  @Override
  public String asString(boolean requirePrefix) {
    return (requirePrefix ? ": " : "") + numberRange.expressAsString();
  }

  @Override
  public boolean test(Tag nbtElement) {
    if (!(nbtElement instanceof final NumericTag nbtNumber))
      return false;
    if (numberRange instanceof BridgeDoubleRange doubleRange) {
      return doubleRange.test(nbtNumber.getAsDouble());
    } else if (numberRange instanceof BridgeFloatRange floatRange) {
      return floatRange.test(nbtNumber.getAsFloat());
    } else if (numberRange instanceof BridgeIntRange intRange) {
      return intRange.test(nbtNumber.getAsInt());
    } else {
      return false;
    }
  }

  @Override
  public NbtPredicateType<RangeNbtPredicate> getType() {
    return NbtPredicateTypes.RANGE;
  }
}
