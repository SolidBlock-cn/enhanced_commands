package pers.solid.ecmd.predicate.pos;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;

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
  @NotNull
  default PosPredicate moved(@NotNull Vec3i relativePos) {
    return newPosPredicateWithTransformation(r -> (R) r.moved(relativePos));
  }

  @Override
  @NotNull
  default PosPredicate moved(@NotNull Vec3d relativePos) {
    return newPosPredicateWithTransformation(r -> (R) r.moved(relativePos));
  }

  @Override
  @NotNull
  default PosPredicate rotated(@NotNull BlockRotation blockRotation, @NotNull Vec3d pivot) {
    return newPosPredicateWithTransformation(r -> (R) r.rotated(blockRotation, pivot));
  }

  @Override
  @NotNull
  default PosPredicate mirrored(Direction.@NotNull Axis axis, @NotNull Vec3d pivot) {
    return newPosPredicateWithTransformation(r -> (R) r.mirrored(axis, pivot));
  }

  @Override
  default PosPredicate transformed(Function<Vec3d, Vec3d> transformation) {
    return newPosPredicateWithTransformation(r -> (R) r.transformed(transformation));
  }
}
