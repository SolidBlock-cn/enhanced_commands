package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;
import java.util.stream.Collectors;

public record AllNbtPredicate(List<NbtPredicate> predicates) implements NbtPredicate {
  public static final MapCodec<AllNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtPredicate.CODEC.listOf().fieldOf("predicates").forGetter(AllNbtPredicate::predicates)
  ).apply(i, AllNbtPredicate::new));

  @Override
  public String expressAsString() {
    return predicates.stream().map(NbtPredicate::expressAsString).collect(Collectors.joining(", ", "all(", ")"));
  }

  @Override
  public boolean test(Tag nbtElement, ExecutionContext context) {
    return predicates.stream().allMatch(p -> p.test(nbtElement, context));
  }

  @Override
  public NbtPredicateType<AllNbtPredicate> getType() {
    return NbtPredicateTypes.ALL;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return predicates;
  }
}
