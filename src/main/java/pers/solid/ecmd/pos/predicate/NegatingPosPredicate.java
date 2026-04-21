package pers.solid.ecmd.pos.predicate;

import net.minecraft.world.phys.Vec3;

public record NegatingPosPredicate(PosPredicate posPredicate) implements PosPredicateBasedPosPredicate<NegatingPosPredicate, PosPredicate> {
  @Override
  public boolean contains(Vec3 vec3d) {
    return !posPredicate.contains(vec3d);
  }

  @Override
  public String expressAsString() {
    if (posPredicate instanceof UnionPosPredicate || posPredicate instanceof IntersectPosPredicate) {
      return "!(" + posPredicate.expressAsString() + ")";
    } else {
      return "!" + posPredicate.expressAsString();
    }
  }

  @Override
  public NegatingPosPredicate newPosPredicate(PosPredicate posPredicate) {
    return new NegatingPosPredicate(posPredicate);
  }
}
