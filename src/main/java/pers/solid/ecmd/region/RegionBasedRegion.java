package pers.solid.ecmd.region;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * 基于另一个区域进行特定的修改而成的区域。
 *
 * @param <T> 该区域自身的类型。
 * @param <R> 该区域所包含的区域的类型。
 */
@SuppressWarnings("unchecked")
public interface RegionBasedRegion<T extends RegionBasedRegion<T, R>, R extends Region> extends Region {
  /**
   * 该区域所使用的区域。对于记录，可以直接将记录组件命令为 {@code region}，从而自动地重写了此方法。
   */
  @Contract(pure = true)
  R region();

  /**
   * 以另一个区域来创建新的区域。
   */
  T newRegion(R region);

  @Override
  @NotNull
  default T moved(@NotNull Vec3i relativePos) {
    return newRegion((R) region().moved(relativePos));
  }

  @Override
  @NotNull
  default T moved(@NotNull Vec3d relativePos) {
    return newRegion((R) region().moved(relativePos));
  }

  @Override
  @NotNull
  default T rotated(@NotNull BlockRotation blockRotation, @NotNull Vec3d pivot) {
    return newRegion((R) region().rotated(blockRotation, pivot));
  }

  @Override
  @NotNull
  default T mirrored(Direction.@NotNull Axis axis, @NotNull Vec3d pivot) {
    return newRegion((R) region().mirrored(axis, pivot));
  }

  @Override
  default T transformed(Function<Vec3d, Vec3d> transformation) {
    return newRegion((R) region().transformed(transformation));
  }

  @Override
  @NotNull
  default T expanded(double offset) {
    return newRegion((R) region().expanded(offset));
  }

  @Override
  @NotNull
  default T expanded(double offset, Direction.Axis axis) {
    return newRegion((R) region().expanded(offset, axis));
  }

  @Override
  @NotNull
  default T expanded(double offset, Direction direction) {
    return newRegion((R) region().expanded(offset, direction));
  }

  @Override
  @NotNull
  default T expanded(double offset, Direction.Type type) {
    return newRegion((R) region().expanded(offset, type));
  }

  interface IntBacked<T extends RegionBasedRegion<T, R>, R extends IntBackedRegion> extends IntBackedRegion, RegionBasedRegion<T, R> {

    @Override
    @NotNull
    default T moved(@NotNull Vec3i relativePos) {
      return newRegion((R) region().moved(relativePos));
    }

    @Override
    @NotNull
    default T moved(@NotNull Vec3d relativePos) {
      return RegionBasedRegion.super.moved(relativePos);
    }

    @Override
    default Region expanded(int offset) {
      return newRegion((R) region().expanded(offset));
    }

    @Override
    default Region expanded(int offset, Direction.Axis axis) {
      return newRegion((R) region().expanded(offset, axis));
    }

    @Override
    default Region expanded(int offset, Direction direction) {
      return newRegion((R) region().expanded(offset, direction));
    }

    @Override
    default Region expanded(int offset, Direction.Type type) {
      return newRegion((R) region().expanded(offset, type));
    }

    @Override
    @NotNull
    default T rotated(@NotNull BlockRotation blockRotation, @NotNull Vec3d pivot) {
      return RegionBasedRegion.super.rotated(blockRotation, pivot);
    }

    @Override
    @NotNull
    default T mirrored(@NotNull Direction.Axis axis, @NotNull Vec3d pivot) {
      return RegionBasedRegion.super.mirrored(axis, pivot);
    }

    @Override
    default T transformed(Function<Vec3d, Vec3d> transformation) {
      return (T) IntBackedRegion.super.transformed(transformation);
    }

    @Override
    default T transformedInt(Function<Vec3i, Vec3i> transformation) {
      return newRegion((R) region().transformedInt(transformation));
    }

    @Override
    @NotNull
    default T expanded(double offset) {
      return RegionBasedRegion.super.expanded(offset);
    }

    @Override
    @NotNull
    default T expanded(double offset, Direction.Axis axis) {
      return RegionBasedRegion.super.expanded(offset, axis);
    }

    @Override
    @NotNull
    default T expanded(double offset, Direction direction) {
      return RegionBasedRegion.super.expanded(offset, direction);
    }

    @Override
    @NotNull
    default T expanded(double offset, Direction.Type type) {
      return RegionBasedRegion.super.expanded(offset, type);
    }
  }
}
