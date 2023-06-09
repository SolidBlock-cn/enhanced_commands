package pers.solid.ecmd.region;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;

/**
 * A region that supports only integer operations. Operations related double will throw {@link UnsupportedOperationException}, unless it equals to the integer.
 */
public interface IntBackedRegion extends Region {
  @Override
  boolean contains(@NotNull Vec3i vec3i);

  @Override
  default boolean contains(@NotNull Vec3d vec3d) {
    return contains(BlockPos.ofFloored(vec3d));
  }

  @Override
  @NotNull
  default Region moved(double count, @NotNull Direction direction) {
    if (count == (int) count) {
      return moved((int) count, direction);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  @NotNull
  Region moved(@NotNull Vec3i relativePos);

  @Override
  @NotNull
  default Region moved(@NotNull Vec3d relativePos) {
    return moved(toIntOrThrow(relativePos));
  }

  @Override
  @NotNull
  default Region expanded(double offset) {
    if (offset == (int) offset) {
      return expanded((int) offset);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  default Region expanded(int offset) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NotNull
  default Region expanded(double offset, Direction.Axis axis) {
    if (offset == (int) offset) {
      return expanded((int) offset, axis);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  default Region expanded(int offset, Direction.Axis axis) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NotNull
  default Region expanded(double offset, Direction direction) {
    if (offset == (int) offset) {
      return expanded((int) offset, direction);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  default Region expanded(int offset, Direction direction) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NotNull
  default Region rotated(@NotNull Vec3d center, @NotNull BlockRotation blockRotation) {
    return rotated(toIntOrThrow(center), blockRotation);
  }

  @NotNull Region rotated(@NotNull Vec3i center, @NotNull BlockRotation blockRotation);

  @Override
  @NotNull
  default Region mirrored(@NotNull Vec3d center, Direction.@NotNull Axis axis) {
    return mirrored(toIntOrThrow(center), axis);
  }

  @NotNull Region mirrored(Vec3i center, Direction.@NotNull Axis axis);

  @Override
  long numberOfBlocksAffected();

  @Override
  default double volume() {
    return numberOfBlocksAffected();
  }

  static Vec3i toIntOrThrow(Vec3d vec3d) {
    final BlockPos vec3i = BlockPos.ofFloored(vec3d);
    if (vec3d.equals(Vec3d.of(vec3i))) {
      return vec3i;
    } else {
      throw new UnsupportedOperationException();
    }
  }
}
