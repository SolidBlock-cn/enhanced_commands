package pers.solid.ecmd.pos.predicate;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
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
  default PosPredicate moved(@NotNull Vec3 relativePos) {
    return newPosPredicate((R) posPredicate().moved(relativePos));
  }

  @Override
  @NotNull
  default PosPredicate rotated(@NotNull Rotation blockRotation, @NotNull Vec3 pivot) {
    return newPosPredicate((R) posPredicate().rotated(blockRotation, pivot));
  }

  @Override
  @NotNull
  default PosPredicate mirrored(Direction.@NotNull Axis axis, @NotNull Vec3 pivot) {
    return newPosPredicate((R) posPredicate().mirrored(axis, pivot));
  }

  @Override
  default PosPredicate transformed(Function<Vec3, Vec3> transformation) {
    return newPosPredicate((R) posPredicate().transformed(transformation));
  }
}
