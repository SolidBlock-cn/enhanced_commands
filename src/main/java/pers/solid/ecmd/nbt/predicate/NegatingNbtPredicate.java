package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;

public record NegatingNbtPredicate(NbtPredicate value) implements NbtPredicate {
  public static final MapCodec<NegatingNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(NbtPredicate.CODEC.fieldOf("value").forGetter(NegatingNbtPredicate::value)).apply(i, NegatingNbtPredicate::new));

  @Override
  public String expressAsString() {
    return "!" + value.expressAsString();
  }

  @Override
  public boolean test(Tag nbtElement, ExecutionContext context) {
    return !value.test(nbtElement, context);
  }

  @Override
  public NbtPredicateType<NegatingNbtPredicate> getType() {
    return NbtPredicateTypes.NEGATING;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(value);
  }
}
