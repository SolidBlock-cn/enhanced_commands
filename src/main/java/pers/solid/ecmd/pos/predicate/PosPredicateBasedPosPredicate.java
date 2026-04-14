package pers.solid.ecmd.pos.predicate;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;

import java.util.function.Function;

@SuppressWarnings("unchecked")
public interface PosPredicateBasedPosPredicate<T extends PosPredicateBasedPosPredicate<T, R>, R extends PosPredicate> extends PosPredicate {
  @Contract(pure = true)
  R posPredicate();

  T newPosPredicate(R posPredicate);

  @Override
  default PosPredicate moved(Vec3i relativePos) {
    return newPosPredicate((R) posPredicate().moved(relativePos));
  }

  @Override
  default PosPredicate moved(Vec3 relativePos) {
    return newPosPredicate((R) posPredicate().moved(relativePos));
  }

  @Override
  default PosPredicate rotated(Rotation blockRotation, Vec3 pivot) {
    return newPosPredicate((R) posPredicate().rotated(blockRotation, pivot));
  }

  @Override
  default PosPredicate mirrored(Direction.Axis axis, Vec3 pivot) {
    return newPosPredicate((R) posPredicate().mirrored(axis, pivot));
  }

  @Override
  default PosPredicate transformed(Function<Vec3, Vec3> transformation) {
    return newPosPredicate((R) posPredicate().transformed(transformation));
  }
}
