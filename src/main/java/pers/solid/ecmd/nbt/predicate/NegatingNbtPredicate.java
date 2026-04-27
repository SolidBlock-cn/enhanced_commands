package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;

public record NegatingNbtPredicate(NbtPredicate value) implements NbtPredicate {
  public static final MapCodec<NegatingNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(NbtPredicate.CODEC.fieldOf("value").forGetter(NegatingNbtPredicate::value)).apply(i, NegatingNbtPredicate::new));

  @Override
  public String expressAsString() {
    return "!" + value.expressAsString();
  }

  @Override
  public boolean test(Tag nbtElement) {
    return !value.test(nbtElement);
  }

  @Override
  public NbtPredicateType<NegatingNbtPredicate> getType() {
    return NbtPredicateTypes.NEGATING;
  }
}
