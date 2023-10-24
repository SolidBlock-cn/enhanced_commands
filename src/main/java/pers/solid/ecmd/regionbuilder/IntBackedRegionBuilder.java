package pers.solid.ecmd.regionbuilder;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.region.IntBackedRegion;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.TextUtil;

import java.util.function.Function;

public interface IntBackedRegionBuilder extends RegionBuilder {
  default void move(@NotNull Vec3i relativePos) {
    transformInt(vec3i -> vec3i.add(relativePos));
  }

  default void move(@NotNull Vec3d relativePos) {
    move(IntBackedRegion.toFlooredIntOrThrow(relativePos, IntBackedRegion.MOVE_MUST_INT_VECTOR));
  }

  default void expand(double offset) {
    if (offset == (int) offset) {
      expand((int) offset);
    } else {
      throw new UnsupportedOperationException(IntBackedRegion.EXPAND_MUST_INT.create(TextUtil.literal(offset).styled(TextUtil.STYLE_FOR_ACTUAL)));
    }
  }

  default void expand(int offset) {
    throw new UnsupportedOperationException();
  }

  default void expand(double offset, Direction.Axis axis) {
    if (offset == (int) offset) {
      expand((int) offset, axis);
    } else {
      throw new UnsupportedOperationException(IntBackedRegion.EXPAND_MUST_INT.create(TextUtil.literal(offset).styled(TextUtil.STYLE_FOR_ACTUAL)));
    }
  }

  default void expand(int offset, Direction.Axis axis) {
    throw new UnsupportedOperationException();
  }

  default void expand(double offset, Direction direction) {
    if (offset == (int) offset) {
      expand((int) offset, direction);
    } else {
      throw new UnsupportedOperationException(IntBackedRegion.EXPAND_MUST_INT.create(TextUtil.literal(offset).styled(TextUtil.STYLE_FOR_ACTUAL)));
    }
  }

  default void expand(int offset, Direction direction) {
    throw new UnsupportedOperationException();
  }

  default void rotate(@NotNull BlockRotation blockRotation, @NotNull Vec3d pivot) {
    rotate(blockRotation, IntBackedRegion.toCenteredIntOrThrow(pivot, IntBackedRegion.ROTATION_PIVOT_MUST_CENTER));
  }

  default void rotate(@NotNull BlockRotation blockRotation, @NotNull Vec3i pivot) {
    transformInt(vec3i -> GeoUtil.rotate(vec3i, blockRotation, pivot));
  }

  default void mirror(Direction.@NotNull Axis axis, @NotNull Vec3d pivot) {
    mirror(axis, IntBackedRegion.toCenteredIntOrThrow(pivot, IntBackedRegion.MIRROR_PIVOT_MUST_CENTER));
  }

  default void mirror(Direction.@NotNull Axis axis, @NotNull Vec3i pivot) {
    transformInt(vec3i -> GeoUtil.mirror(vec3i, axis, pivot));
  }

  @Override
  default void transform(Function<Vec3d, Vec3d> transformation) {
    throw new UnsupportedOperationException();
  }

  void transformInt(Function<Vec3i, Vec3i> transformation);
}
