package pers.solid.ecmd.region;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public interface RegionsBasedRegion<T extends RegionsBasedRegion<T, R>, R extends Region> extends Region {
  Collection<R> regions();

  T newRegion(Collection<R> regions);

  default T newRegionWithTransformation(com.google.common.base.Function<R, R> transformation) {
    return newRegion(ImmutableList.copyOf(Collections2.transform(regions(), transformation)));
  }

  @Override
  @NotNull
  default T moved(@NotNull Vec3i relativePos) {
    return newRegionWithTransformation(input -> (R) input.moved(relativePos));
  }

  @Override
  @NotNull
  default T moved(@NotNull Vec3d relativePos) {
    return newRegionWithTransformation(input -> (R) input.moved(relativePos));
  }

  @Override
  @NotNull
  default T rotated(@NotNull BlockRotation blockRotation, @NotNull Vec3d pivot) {
    return newRegionWithTransformation(input -> (R) input.rotated(blockRotation, pivot));
  }

  @Override
  @NotNull
  default T mirrored(Direction.@NotNull Axis axis, @NotNull Vec3d pivot) {
    return newRegionWithTransformation(input -> (R) input.mirrored(axis, pivot));
  }

  @Override
  default T transformed(Function<Vec3d, Vec3d> transformation) {
    return newRegionWithTransformation(input -> (R) input.transformed(transformation));
  }

  @Override
  @NotNull
  default T expanded(double offset) {
    return newRegionWithTransformation(input -> (R) input.expanded(offset));
  }

  @Override
  @NotNull
  default T expanded(double offset, Direction.Axis axis) {
    return newRegionWithTransformation(input -> (R) input.expanded(offset, axis));
  }

  @Override
  @NotNull
  default T expanded(double offset, Direction direction) {
    return newRegionWithTransformation(input -> (R) input.expanded(offset, direction));
  }

  @Override
  @NotNull
  default T expanded(double offset, Direction.Type type) {
    return newRegionWithTransformation(input -> (R) input.expanded(offset, type));
  }
}
