package pers.solid.ecmd.regionselection;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.region.IntBackedRegion;
import pers.solid.ecmd.util.GeoUtil;

import java.util.function.Function;

public interface IntBackedRegionSelection extends RegionSelection {

  default @NotNull RegionSelection moved(@NotNull Vec3i relativePos) {
    return transformedInt(vec3i -> vec3i.offset(relativePos));
  }

  default @NotNull RegionSelection moved(@NotNull Vec3 relativePos) {
    return moved(IntBackedRegion.toFlooredIntOrThrow(relativePos, IntBackedRegion.MOVE_MUST_INT_VECTOR));
  }

  default @NotNull RegionSelection expanded(double offset) throws CommandSyntaxException {
    expanded((int) offset);
    return this;
  }

  @NotNull RegionSelection expanded(int offset) throws CommandSyntaxException;

  default @NotNull RegionSelection expanded(double offset, Direction.Axis axis) throws CommandSyntaxException {
    expanded((int) offset, axis);
    return this;
  }

  RegionSelection expanded(int offset, Direction.Axis axis) throws CommandSyntaxException;

  default @NotNull RegionSelection expanded(double offset, Direction direction) throws CommandSyntaxException {
    expanded((int) offset, direction);
    return this;
  }

  @NotNull RegionSelection expanded(int offset, Direction direction) throws CommandSyntaxException;

  @Override
  default @NotNull RegionSelection expanded(double offset, Direction.Plane type) throws CommandSyntaxException {
    expanded((int) offset, type);
    return this;
  }

  @NotNull RegionSelection expanded(int offset, Direction.Plane type) throws CommandSyntaxException;

  default @NotNull RegionSelection rotated(@NotNull Rotation blockRotation, @NotNull Vec3 pivot) {
    return rotated(blockRotation, IntBackedRegion.toCenteredIntOrThrow(pivot, IntBackedRegion.ROTATION_PIVOT_MUST_CENTER));
  }

  default @NotNull RegionSelection rotated(@NotNull Rotation blockRotation, @NotNull Vec3i pivot) {
    return transformedInt(vec3i -> GeoUtil.rotate(vec3i, blockRotation, pivot));
  }

  default @NotNull RegionSelection mirrored(Direction.@NotNull Axis axis, @NotNull Vec3 pivot) {
    return mirrored(axis, IntBackedRegion.toCenteredIntOrThrow(pivot, IntBackedRegion.MIRROR_PIVOT_MUST_CENTER));
  }

  default @NotNull RegionSelection mirrored(Direction.@NotNull Axis axis, @NotNull Vec3i pivot) {
    return transformedInt(vec3i -> GeoUtil.mirror(vec3i, axis, pivot));
  }

  @Override
  default @NotNull IntBackedRegionSelection transformed(Function<Vec3, Vec3> transformation) {
    transformedInt(vec3i -> BlockPos.containing(transformation.apply(Vec3.atCenterOf(vec3i))));
    return this;
  }

  @NotNull IntBackedRegionSelection transformedInt(Function<Vec3i, Vec3i> transformation);

  @Override
  @NotNull IntBackedRegionSelection clone();

  @Override
  IntBackedRegion region();
}
