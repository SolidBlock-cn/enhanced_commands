package pers.solid.ecmd.regionselection;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.region.IntBackedRegion;
import pers.solid.ecmd.util.GeoUtil;

import java.util.function.Function;

public interface IntBackedRegionSelection extends RegionSelection {

  default @NotNull RegionSelection moved(@NotNull Vec3i relativePos) {
    return transformedInt(vec3i -> vec3i.add(relativePos));
  }

  default @NotNull RegionSelection moved(@NotNull Vec3d relativePos) {
    return moved(IntBackedRegion.toFlooredIntOrThrow(relativePos, IntBackedRegion.MOVE_MUST_INT_VECTOR));
  }

  default @NotNull RegionSelection expanded(double offset) {
    expanded((int) offset);
    return this;
  }

  @NotNull RegionSelection expanded(int offset);

  default @NotNull RegionSelection expanded(double offset, Direction.Axis axis) {
    expanded((int) offset, axis);
    return this;
  }

  RegionSelection expanded(int offset, Direction.Axis axis);

  default @NotNull RegionSelection expanded(double offset, Direction direction) {
    expanded((int) offset, direction);
    return this;
  }

  @NotNull RegionSelection expanded(int offset, Direction direction);

  @Override
  default @NotNull RegionSelection expanded(double offset, Direction.Type type) {
    expanded((int) offset, type);
    return this;
  }

  @NotNull RegionSelection expanded(int offset, Direction.Type type);

  default @NotNull RegionSelection rotated(@NotNull BlockRotation blockRotation, @NotNull Vec3d pivot) {
    return rotated(blockRotation, IntBackedRegion.toCenteredIntOrThrow(pivot, IntBackedRegion.ROTATION_PIVOT_MUST_CENTER));
  }

  default @NotNull RegionSelection rotated(@NotNull BlockRotation blockRotation, @NotNull Vec3i pivot) {
    return transformedInt(vec3i -> GeoUtil.rotate(vec3i, blockRotation, pivot));
  }

  default @NotNull RegionSelection mirrored(Direction.@NotNull Axis axis, @NotNull Vec3d pivot) {
    return mirrored(axis, IntBackedRegion.toCenteredIntOrThrow(pivot, IntBackedRegion.MIRROR_PIVOT_MUST_CENTER));
  }

  default @NotNull RegionSelection mirrored(Direction.@NotNull Axis axis, @NotNull Vec3i pivot) {
    return transformedInt(vec3i -> GeoUtil.mirror(vec3i, axis, pivot));
  }

  @Override
  default @NotNull IntBackedRegionSelection transformed(Function<Vec3d, Vec3d> transformation) {
    transformedInt(vec3i -> BlockPos.ofFloored(transformation.apply(Vec3d.ofCenter(vec3i))));
    return this;
  }

  @NotNull IntBackedRegionSelection transformedInt(Function<Vec3i, Vec3i> transformation);

  @Override
  @NotNull IntBackedRegionSelection clone();

  @Override
  IntBackedRegion region();
}
