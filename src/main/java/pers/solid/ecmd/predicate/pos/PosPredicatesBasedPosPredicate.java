package pers.solid.ecmd.predicate.pos;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
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
  default PosPredicate moved(@NotNull Vec3 relativePos) {
    return newPosPredicateWithTransformation(r -> (R) r.moved(relativePos));
  }

  @Override
  @NotNull
  default PosPredicate rotated(@NotNull Rotation blockRotation, @NotNull Vec3 pivot) {
    return newPosPredicateWithTransformation(r -> (R) r.rotated(blockRotation, pivot));
  }

  @Override
  @NotNull
  default PosPredicate mirrored(@NotNull Direction.Axis axis, @NotNull Vec3 pivot) {
    return newPosPredicateWithTransformation(r -> (R) r.mirrored(axis, pivot));
  }

  @Override
  default PosPredicate transformed(Function<Vec3, Vec3> transformation) {
    return newPosPredicateWithTransformation(r -> (R) r.transformed(transformation));
  }
}
