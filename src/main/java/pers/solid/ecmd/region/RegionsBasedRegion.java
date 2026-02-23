package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public interface RegionsBasedRegion<T extends RegionsBasedRegion<T, R>, R extends Region> extends Region {
  static <R extends Region, T extends RegionsBasedRegion<T, R>> RecordCodecBuilder<T, List<R>> regionsCodecField(Codec<R> codec) {
    return codec.listOf().fieldOf("regions").forGetter(RegionsBasedRegion::regions);
  }

  @NotNull
  List<R> regions();

  T newRegion(@NotNull List<R> regions);

  default T newRegionWithTransformation(Function<R, R> transformation) {
    return newRegion(regions().stream().map(transformation).toList());
  }

  @Override
  @NotNull
  default T moved(@NotNull Vec3i relativePos) {
    return newRegionWithTransformation(input -> (R) input.moved(relativePos));
  }

  @Override
  @NotNull
  default T moved(@NotNull Vec3 relativePos) {
    return newRegionWithTransformation(input -> (R) input.moved(relativePos));
  }

  @Override
  @NotNull
  default T rotated(@NotNull Rotation blockRotation, @NotNull Vec3 pivot) {
    return newRegionWithTransformation(input -> (R) input.rotated(blockRotation, pivot));
  }

  @Override
  @NotNull
  default T mirrored(Direction.@NotNull Axis axis, @NotNull Vec3 pivot) {
    return newRegionWithTransformation(input -> (R) input.mirrored(axis, pivot));
  }

  @Override
  default @NotNull T transformed(Function<Vec3, Vec3> transformation) {
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
  default T expanded(double offset, Direction.Plane type) {
    return newRegionWithTransformation(input -> (R) input.expanded(offset, type));
  }
}
