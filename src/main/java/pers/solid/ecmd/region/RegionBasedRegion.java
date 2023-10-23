package pers.solid.ecmd.region;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * 基于另一个区域进行特定的修改而成的区域。
 *
 * @param <T> 该区域自身的类型。
 * @param <R> 该区域所包含的区域的类型。
 */
public interface RegionBasedRegion<T extends RegionBasedRegion<T, R>, R extends Region> extends Region {
  /**
   * 该区域所使用的区域。对于记录，可以直接将记录组件命令为 {@code region}，从而自动地重写了此方法。
   */
  @Contract(pure = true)
  R region();

  /**
   * 以另一个区域来创建新的区域。
   */
  T newRegion(Region region);

  @Override
  @NotNull
  default T moved(int count, @NotNull Direction direction) {
    return newRegion(region().moved(count, direction));
  }

  @Override
  @NotNull
  default T moved(double count, @NotNull Direction direction) {
    return newRegion(region().moved(count, direction));
  }

  @Override
  @NotNull
  default T moved(@NotNull Vec3i relativePos) {
    return newRegion(region().moved(relativePos));
  }

  @Override
  @NotNull
  default T moved(@NotNull Vec3d relativePos) {
    return newRegion(region().moved(relativePos));
  }

  @Override
  @NotNull
  default T rotated(@NotNull Vec3d pivot, @NotNull BlockRotation blockRotation) {
    return newRegion(region().rotated(pivot, blockRotation));
  }

  @Override
  @NotNull
  default T mirrored(@NotNull Vec3d pivot, Direction.@NotNull Axis axis) {
    return newRegion(region().mirrored(pivot, axis));
  }

  @Override
  @NotNull
  default T expanded(double offset) {
    return newRegion(region().expanded(offset));
  }

  @Override
  @NotNull
  default T expanded(double offset, Direction.Axis axis) {
    return newRegion(region().expanded(offset, axis));
  }

  @Override
  @NotNull
  default T expanded(double offset, Direction direction) {
    return newRegion(region().expanded(offset, direction));
  }
}
