package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public interface RegionsBasedRegion<T extends RegionsBasedRegion<T, R>, R extends Region> extends Region {
  static <R extends Region, T extends RegionsBasedRegion<T, R>> RecordCodecBuilder<T, List<R>> regionsCodecField(Codec<R> codec) {
    return codec.listOf().fieldOf("regions").forGetter(RegionsBasedRegion::regions);
  }

  List<R> regions();

  T newRegion(List<R> regions);

  default T newRegionWithTransformation(Function<R, R> transformation) {
    return newRegion(regions().stream().map(transformation).toList());
  }

  @Override
  default T moved(Vec3i relativePos) {
    return newRegionWithTransformation(input -> (R) input.moved(relativePos));
  }

  @Override
  default T moved(Vec3 relativePos) {
    return newRegionWithTransformation(input -> (R) input.moved(relativePos));
  }

  @Override
  default T rotated(Rotation blockRotation, Vec3 pivot) {
    return newRegionWithTransformation(input -> (R) input.rotated(blockRotation, pivot));
  }

  @Override
  default T mirrored(Direction.Axis axis, Vec3 pivot) {
    return newRegionWithTransformation(input -> (R) input.mirrored(axis, pivot));
  }

  @Override
  default T transformed(Function<Vec3, Vec3> transformation) {
    return newRegionWithTransformation(input -> (R) input.transformed(transformation));
  }

  @Override
  default T expanded(double offset) {
    return newRegionWithTransformation(input -> (R) input.expanded(offset));
  }

  @Override
  default T expanded(double offset, Direction.Axis axis) {
    return newRegionWithTransformation(input -> (R) input.expanded(offset, axis));
  }

  @Override
  default T expanded(double offset, Direction direction) {
    return newRegionWithTransformation(input -> (R) input.expanded(offset, direction));
  }

  @Override
  default T expanded(double offset, Direction.Plane type) {
    return newRegionWithTransformation(input -> (R) input.expanded(offset, type));
  }
}
