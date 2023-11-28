package pers.solid.ecmd.predicate.pos;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@SuppressWarnings("unchecked")
public interface PosPredicateBasedPosPredicate<T extends PosPredicateBasedPosPredicate<T, R>, R extends PosPredicate> extends PosPredicate {
  @Contract(pure = true)
  R posPredicate();

  T newPosPredicate(R posPredicate);

  @Override
  @NotNull
  default PosPredicate moved(@NotNull Vec3i relativePos) {
    return newPosPredicate((R) posPredicate().moved(relativePos));
  }

  @Override
  @NotNull
  default PosPredicate moved(@NotNull Vec3d relativePos) {
    return newPosPredicate((R) posPredicate().moved(relativePos));
  }

  @Override
  @NotNull
  default PosPredicate rotated(@NotNull BlockRotation blockRotation, @NotNull Vec3d pivot) {
    return newPosPredicate((R) posPredicate().rotated(blockRotation, pivot));
  }

  @Override
  @NotNull
  default PosPredicate mirrored(Direction.@NotNull Axis axis, @NotNull Vec3d pivot) {
    return newPosPredicate((R) posPredicate().mirrored(axis, pivot));
  }

  @Override
  default PosPredicate transformed(Function<Vec3d, Vec3d> transformation) {
    return newPosPredicate((R) posPredicate().transformed(transformation));
  }
}
