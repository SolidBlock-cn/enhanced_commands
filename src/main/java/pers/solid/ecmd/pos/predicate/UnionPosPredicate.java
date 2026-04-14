package pers.solid.ecmd.pos.predicate;

import com.google.common.collect.Iterables;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

public record UnionPosPredicate(Collection<PosPredicate> posPredicates) implements PosPredicatesBasedPosPredicate<UnionPosPredicate, PosPredicate> {
  @Override
  public boolean contains(@NotNull Vec3 vec3d) {
    return Iterables.any(posPredicates, input -> input.contains(vec3d));
  }

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return Iterables.any(posPredicates, input -> input.contains(vec3i));
  }

  @Override
  public @NotNull String asString() {
    return posPredicates.stream().map(PosPredicate::asString).collect(Collectors.joining("|", "(", ")"));
  }

  @Override
  public UnionPosPredicate newPosPredicate(Collection<PosPredicate> posPredicates) {
    return new UnionPosPredicate(posPredicates);
  }
}
