package pers.solid.ecmd.pos.predicate;

import com.google.common.collect.Iterables;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.stream.Collectors;

public record IntersectPosPredicate(Collection<PosPredicate> posPredicates) implements PosPredicatesBasedPosPredicate<IntersectPosPredicate, PosPredicate> {
  @Override
  public boolean contains(Vec3 vec3d) {
    return Iterables.all(posPredicates, input -> input.contains(vec3d));
  }

  @Override
  public String expressAsString() {
    return posPredicates.stream().map(posPredicate -> posPredicate instanceof UnionPosPredicate unionPosPredicate ? unionPosPredicate.posPredicates().stream().map(PosPredicate::expressAsString).collect(Collectors.joining("|", "(", ")")) : posPredicate.expressAsString()).collect(Collectors.joining("&"));
  }

  @Override
  public IntersectPosPredicate newPosPredicate(Collection<PosPredicate> posPredicates) {
    return new IntersectPosPredicate(posPredicates);
  }
}
