package pers.solid.ecmd.predicate.nbt;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

public record NegatingNbtPredicate(NbtPredicate predicate) implements NbtPredicate {
  public static final MapCodec<NegatingNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(NbtPredicate.CODEC.fieldOf("predicate").forGetter(NegatingNbtPredicate::predicate)).apply(i, NegatingNbtPredicate::new));

  @Override
  public @NotNull String asString() {
    return "!" + predicate.asString();
  }

  @Override
  public boolean test(@NotNull Tag nbtElement) {
    return !predicate.test(nbtElement);
  }

  @Override
  public @NotNull NbtPredicateType<NegatingNbtPredicate> getType() {
    return NbtPredicateTypes.NEGATING;
  }

  public enum Type implements NbtPredicateType<NegatingNbtPredicate> {
    NEGATING_TYPE;

    @Override
    public MapCodec<NegatingNbtPredicate> getCodec() {
      return null;
    }
  }
}
