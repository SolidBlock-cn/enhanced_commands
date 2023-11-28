package pers.solid.ecmd.curve;

import com.google.common.collect.Streams;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.GeoUtil;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Curve 是指可以绘制的曲线。与 {@link Region} 不同，曲线没有面积，也无法判断某个坐标是否在这个线内。但是，曲线仍然可以对点进行迭代。不同的是，对于曲线而言，产生迭代的点需要指定一个间距（这个间距可能会在迭代的过程中发生改变），否则会不知道如何进行迭代。
 */
public interface Curve extends Region {
  /**
   * 曲线由于是没有粗细，所以一般不能判断点是否在该曲线内。。
   */
  @Override
  default boolean contains(@NotNull Vec3d vec3d) {
    return false;
  }

  /**
   * 沿着这个曲线产生一条像素化的连续的线。这个 BlockPos 有可能是 {@link BlockPos.Mutable}。
   */
  default @NotNull Iterator<BlockPos> iterator() {
    return streamBlockPos().iterator();
  }

  /**
   * 沿着这条线，按一定的距离产生点。这个距离可能随着迭代而改变，从而用于产生“点划线”等特殊形状的虚线。
   *
   * @param interval 间距，可变，例如 {@link MutableDouble}。但是在迭代的过程中不应该修改它，而应该由调用者来修改。
   */
  @NotNull Iterator<Vec3d> iteratePoints(Number interval);

  /**
   * 沿着这条线，按恒定的距离产生点。
   *
   * @param interval 恒定的间距。
   */
  @ApiStatus.NonExtendable
  default @NotNull Iterator<Vec3d> iteratePoints(double interval) {
    return iteratePoints(Double.valueOf(interval));
  }

  /**
   * 沿着这个曲线产生一条像素化的连续的线。这个 BlockPos 有可能是 {@link BlockPos.Mutable}。
   *
   * @implNote 此方法会被 {@link #iterator()} 使用。如果覆盖了 {@link #iterator()}，那么应该一并覆盖此方法。
   */
  default @NotNull Stream<BlockPos> streamBlockPos() {
    return streamPoints(0.1d).map(BlockPos::ofFloored).distinct();
  }

  /**
   * 沿着这条线，按一定的距离产生点。这个距离可能随着迭代而改变，从而用于产生“点划线”等特殊形状的虚线。
   *
   * @param interval 间距，可变，例如 {@link MutableDouble}。但是在迭代的过程中不应该修改它，而应该由调用者来修改。
   */
  default @NotNull Stream<Vec3d> streamPoints(Number interval) {
    Iterable<Vec3d> iterable = () -> iteratePoints(interval);
    return Streams.stream(iterable);
  }

  @ApiStatus.NonExtendable
  default @NotNull Stream<Vec3d> streamPoints(double interval) {
    return streamPoints(Double.valueOf(interval));
  }

  double length();

  @Override
  @NotNull
  Curve transformed(Function<Vec3d, Vec3d> transformation);

  default @NotNull Curve moved(double count, @NotNull Direction direction) {
    return moved(Vec3d.of(direction.getVector()).multiply(count));
  }


  @NotNull
  default Curve moved(@NotNull Vec3d relativePos) {
    return transformed(vec3d -> vec3d.add(relativePos));
  }

  @NotNull
  default Curve rotated(@NotNull BlockRotation blockRotation, @NotNull Vec3d pivot) {
    return transformed(vec3d -> GeoUtil.rotate(vec3d, blockRotation, pivot));
  }

  @NotNull
  default Curve mirrored(@NotNull Direction.Axis axis, @NotNull Vec3d pivot) {
    return transformed(vec3d -> GeoUtil.mirror(vec3d, axis, pivot));
  }

  @Override
  @NotNull String asString();

  @NotNull CurveType<?> getType();

  /**
   * 由于曲线没有粗细，故只能与长度估算。
   */
  @Override
  default double volume() {
    return length();
  }
}
