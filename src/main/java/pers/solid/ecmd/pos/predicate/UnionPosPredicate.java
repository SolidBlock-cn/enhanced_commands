package pers.solid.ecmd.pos.predicate;

import com.google.common.collect.Iterables;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.stream.Collectors;

public record UnionPosPredicate(Collection<PosPredicate> posPredicates) implements PosPredicatesBasedPosPredicate<UnionPosPredicate, PosPredicate> {
  @Override
  public boolean contains(Vec3 vec3d) {
    return Iterables.any(posPredicates, input -> input.contains(vec3d));
  }

  @Override
  public boolean contains(Vec3i vec3i) {
    return Iterables.any(posPredicates, input -> input.contains(vec3i));
  }

  @Override
  public String expressAsString() {
    return posPredicates.stream().map(PosPredicate::expressAsString).collect(Collectors.joining("|", "(", ")"));
  }

  @Override
  public UnionPosPredicate newPosPredicate(Collection<PosPredicate> posPredicates) {
    return new UnionPosPredicate(posPredicates);
  }
}
