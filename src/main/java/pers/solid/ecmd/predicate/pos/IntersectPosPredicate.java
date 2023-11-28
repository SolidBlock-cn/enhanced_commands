package pers.solid.ecmd.predicate.pos;

import com.google.common.collect.Iterables;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

public record IntersectPosPredicate(Collection<PosPredicate> posPredicates) implements PosPredicatesBasedPosPredicate<IntersectPosPredicate, PosPredicate> {
  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return Iterables.all(posPredicates, input -> input.contains(vec3d));
  }

  @Override
  public @NotNull String asString() {
    return posPredicates.stream().map(posPredicate -> posPredicate instanceof UnionPosPredicate unionPosPredicate ? unionPosPredicate.posPredicates().stream().map(PosPredicate::asString).collect(Collectors.joining("|", "(", ")")) : posPredicate.asString()).collect(Collectors.joining("&"));
  }

  @Override
  public IntersectPosPredicate newPosPredicate(Collection<PosPredicate> posPredicates) {
    return new IntersectPosPredicate(posPredicates);
  }
}
