package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

public enum ConstantNbtPredicate implements NbtPredicate {
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
  public @NotNull String asString() {
    return asString(false);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (value ? "" : "!") + (requirePrefix ? ": " : "") + "*";
  }

  @Override
  public boolean test(@NotNull Tag nbtElement) {
    return value;
  }

  @Override
  public @NotNull NbtPredicateType<ConstantNbtPredicate> getType() {
    return ConstantNbtPredicate.Type.CONSTANT_TYPE;
  }

  public static ConstantNbtPredicate of(boolean value) {
    return value ? TRUE : FALSE;
  }

  public enum Type implements NbtPredicateType<ConstantNbtPredicate> {
    CONSTANT_TYPE;

    @Override
    public MapCodec<ConstantNbtPredicate> getCodec() {
      return CODEC;
    }
  }
}
