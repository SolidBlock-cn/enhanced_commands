package pers.solid.ecmd.pos.predicate;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public interface PosPredicatesBasedPosPredicate<T extends PosPredicatesBasedPosPredicate<T, R>, R extends PosPredicate> extends PosPredicate {
  Collection<R> posPredicates();

  T newPosPredicate(Collection<R> posPredicates);

  default T newPosPredicateWithTransformation(Function<R, R> transformation) {
    return newPosPredicate(posPredicates().stream().map(transformation).toList());
  }

  @Override
  default PosPredicate moved(Vec3i relativePos) {
    return newPosPredicateWithTransformation(r -> (R) r.moved(relativePos));
  }

  @Override
  default PosPredicate moved(Vec3 relativePos) {
    return newPosPredicateWithTransformation(r -> (R) r.moved(relativePos));
  }

  @Override
  default PosPredicate rotated(Rotation blockRotation, Vec3 pivot) {
    return newPosPredicateWithTransformation(r -> (R) r.rotated(blockRotation, pivot));
  }

  @Override
  default PosPredicate mirrored(Direction.Axis axis, Vec3 pivot) {
    return newPosPredicateWithTransformation(r -> (R) r.mirrored(axis, pivot));
  }

  @Override
  default PosPredicate transformed(Function<Vec3, Vec3> transformation) {
    return newPosPredicateWithTransformation(r -> (R) r.transformed(transformation));
  }
}
