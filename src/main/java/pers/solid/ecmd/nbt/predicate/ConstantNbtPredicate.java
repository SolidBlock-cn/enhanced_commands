package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

public enum ConstantNbtPredicate implements NbtPredicate, DoesNotRequireValidation {
  TRUE(true),
  FALSE(false);

  public static final MapCodec<ConstantNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.BOOL.fieldOf("value").forGetter(o -> o == TRUE)
  ).apply(i, b -> b ? TRUE : FALSE));
  private final boolean value;

  ConstantNbtPredicate(boolean value) {
    this.value = value;
  }

  @Override
  public String expressAsString() {
    return asString(false);
  }

  @Override
  public String asString(boolean requirePrefix) {
    return (value ? "" : "!") + (requirePrefix ? ": " : "") + "*";
  }

  @Override
  public boolean test(Tag nbtElement) {
    return value;
  }

  @Override
  public NbtPredicate negate() {
    return of(!value);
  }

  @Override
  public NbtPredicateType<ConstantNbtPredicate> getType() {
    return NbtPredicateTypes.CONSTANT;
  }

  public static ConstantNbtPredicate of(boolean value) {
    return value ? TRUE : FALSE;
  }
}
