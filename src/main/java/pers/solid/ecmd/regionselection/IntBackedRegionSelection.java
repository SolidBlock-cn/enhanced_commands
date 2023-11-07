package pers.solid.ecmd.regionselection;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.region.IntBackedRegion;
import pers.solid.ecmd.util.GeoUtil;

import java.util.function.Function;

public interface IntBackedRegionSelection extends RegionSelection, IntBackedRegion {
  @Override
  default boolean contains(@NotNull Vec3i vec3i) {
    return region().contains(vec3i);
  }

  default @NotNull RegionSelection moved(@NotNull Vec3i relativePos) {
    return transformedInt(vec3i -> vec3i.add(relativePos));
  }

  default @NotNull RegionSelection moved(@NotNull Vec3d relativePos) {
    return moved(IntBackedRegion.toFlooredIntOrThrow(relativePos, IntBackedRegion.MOVE_MUST_INT_VECTOR));
  }

  default @NotNull RegionSelection expanded(double offset) {
    IntBackedRegion.super.expanded(offset);
    return this;
  }

  @Override
  default @NotNull RegionSelection expanded(int offset) {
    throw new UnsupportedOperationException();
  }

  default @NotNull RegionSelection expanded(double offset, Direction.Axis axis) {
    IntBackedRegion.super.expanded(offset, axis);
    return this;
  }

  default RegionSelection expanded(int offset, Direction.Axis axis) {
    throw new UnsupportedOperationException();
  }

  default @NotNull RegionSelection expanded(double offset, Direction direction) {
    IntBackedRegion.super.expanded(offset, direction);
    return this;
  }

  default @NotNull RegionSelection expanded(int offset, Direction direction) {
    throw new UnsupportedOperationException();
  }

  @Override
  default @NotNull RegionSelection expanded(double offset, Direction.Type type) {
    IntBackedRegion.super.expanded(offset, type);
    return this;
  }

  @Override
  default boolean contains(@NotNull Vec3d vec3d) {
    return RegionSelection.super.contains(vec3d);
  }

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
  default IntBackedRegionSelection transformed(Function<Vec3d, Vec3d> transformation) {
    IntBackedRegion.super.transformed(transformation);
    return this;
  }

  @Override
  @NotNull IntBackedRegionSelection transformedInt(Function<Vec3i, Vec3i> transformation);

  @Override
  @NotNull IntBackedRegionSelection clone();

  @Override
  IntBackedRegion region();

  @Override
  default long numberOfBlocksAffected() {
    return region().numberOfBlocksAffected();
  }

  @Override
  default @Nullable BlockBox minContainingBlockBox() {
    return region().minContainingBlockBox();
  }

  @Override
  default @Nullable Box minContainingBox() {
    return region().minContainingBox();
  }

  @Override
  default double volume() {
    return region().volume();
  }
}
