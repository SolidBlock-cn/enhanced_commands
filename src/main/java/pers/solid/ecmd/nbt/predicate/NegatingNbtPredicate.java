package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;

public record NegatingNbtPredicate(NbtPredicate predicate) implements NbtPredicate {
  public static final MapCodec<NegatingNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(NbtPredicate.CODEC.fieldOf("predicate").forGetter(NegatingNbtPredicate::predicate)).apply(i, NegatingNbtPredicate::new));

  @Override
  public String expressAsString() {
    return "!" + predicate.expressAsString();
  }

  @Override
  public boolean test(Tag nbtElement) {
    return !predicate.test(nbtElement);
  }

  @Override
  public NbtPredicateType<NegatingNbtPredicate> getType() {
    return NbtPredicateTypes.NEGATING;
  }
}
