package pers.solid.ecmd.predicate.pos;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public record NegatingPosPredicate(PosPredicate posPredicate) implements PosPredicateBasedPosPredicate<NegatingPosPredicate, PosPredicate> {
  @Override
  public boolean contains(@NotNull Vec3d vec3d) {
    return !posPredicate.contains(vec3d);
  }

  @Override
  public @NotNull String asString() {
    if (posPredicate instanceof UnionPosPredicate || posPredicate instanceof IntersectPosPredicate) {
      return "!(" + posPredicate.asString() + ")";
    } else {
      return "!" + posPredicate.asString();
    }
  }

  @Override
  public NegatingPosPredicate newPosPredicate(PosPredicate posPredicate) {
    return new NegatingPosPredicate(posPredicate);
  }
}
