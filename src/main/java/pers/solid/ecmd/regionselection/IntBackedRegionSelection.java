package pers.solid.ecmd.regionselection;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.region.IntBackedRegion;
import pers.solid.ecmd.util.GeoUtil;

import java.util.function.Function;

public interface IntBackedRegionSelection extends RegionSelection {

  default RegionSelection moved(Vec3i relativePos) {
    return transformedInt(vec3i -> vec3i.offset(relativePos));
  }

  default RegionSelection moved(Vec3 relativePos) {
    return moved(IntBackedRegion.toFlooredIntOrThrow(relativePos, IntBackedRegion.MOVE_MUST_INT_VECTOR));
  }

  default RegionSelection expanded(double offset) throws CommandSyntaxException {
    expanded((int) offset);
    return this;
  }

  RegionSelection expanded(int offset) throws CommandSyntaxException;

  default RegionSelection expanded(double offset, Direction.Axis axis) throws CommandSyntaxException {
    expanded((int) offset, axis);
    return this;
  }

  RegionSelection expanded(int offset, Direction.Axis axis) throws CommandSyntaxException;

  default RegionSelection expanded(double offset, Direction direction) throws CommandSyntaxException {
    expanded((int) offset, direction);
    return this;
  }

  RegionSelection expanded(int offset, Direction direction) throws CommandSyntaxException;

  @Override
  default RegionSelection expanded(double offset, Direction.Plane type) throws CommandSyntaxException {
    expanded((int) offset, type);
    return this;
  }

  RegionSelection expanded(int offset, Direction.Plane type) throws CommandSyntaxException;

  default RegionSelection rotated(Rotation blockRotation, Vec3 pivot) {
    return rotated(blockRotation, IntBackedRegion.toCenteredIntOrThrow(pivot, IntBackedRegion.ROTATION_PIVOT_MUST_CENTER));
  }

  default RegionSelection rotated(Rotation blockRotation, Vec3i pivot) {
    return transformedInt(vec3i -> GeoUtil.rotate(vec3i, blockRotation, pivot));
  }

  default RegionSelection mirrored(Direction.Axis axis, Vec3 pivot) {
    return mirrored(axis, IntBackedRegion.toCenteredIntOrThrow(pivot, IntBackedRegion.MIRROR_PIVOT_MUST_CENTER));
  }

  default RegionSelection mirrored(Direction.Axis axis, Vec3i pivot) {
    return transformedInt(vec3i -> GeoUtil.mirror(vec3i, axis, pivot));
  }

  @Override
  default IntBackedRegionSelection transformed(Function<Vec3, Vec3> transformation) {
    return transformedInt(vec3i -> BlockPos.containing(transformation.apply(Vec3.atCenterOf(vec3i))));
  }

  IntBackedRegionSelection transformedInt(Function<Vec3i, Vec3i> transformation);

  @Override
  IntBackedRegionSelection clone();

  @Override
  IntBackedRegion region();
}
