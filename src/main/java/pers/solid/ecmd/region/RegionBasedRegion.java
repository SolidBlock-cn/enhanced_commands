package pers.solid.ecmd.region;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
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
  default T moved(Vec3i relativePos) {
    return newRegion((R) region().moved(relativePos));
  }

  @Override
  default T moved(Vec3 relativePos) {
    return newRegion((R) region().moved(relativePos));
  }

  @Override
  default T rotated(Rotation blockRotation, Vec3 pivot) {
    return newRegion((R) region().rotated(blockRotation, pivot));
  }

  @Override
  default T mirrored(Direction.Axis axis, Vec3 pivot) {
    return newRegion((R) region().mirrored(axis, pivot));
  }

  @Override
  default T transformed(Function<Vec3, Vec3> transformation) {
    return newRegion((R) region().transformed(transformation));
  }

  @Override
  default T expanded(double offset) {
    return newRegion((R) region().expanded(offset));
  }

  @Override
  default T expanded(double offset, Direction.Axis axis) {
    return newRegion((R) region().expanded(offset, axis));
  }

  @Override
  default T expanded(double offset, Direction direction) {
    return newRegion((R) region().expanded(offset, direction));
  }

  @Override
  default T expanded(double offset, Direction.Plane type) {
    return newRegion((R) region().expanded(offset, type));
  }

  @Override
  default boolean contains(Vec3 vec3d) {
    return region().contains(vec3d);
  }

  @Override
  default long numberOfBlocksAffected() {
    return region().numberOfBlocksAffected();
  }

  @Override
  default double volume() {
    return region().volume();
  }

  @Override
  default @Nullable BoundingBox minContainingBlockBox() {
    return region().minContainingBlockBox();
  }

  @Override
  @Nullable
  default AABB minContainingBox() {
    return region().minContainingBox();
  }

  @Override
  default Iterator<BlockPos> iterator() {
    return region().iterator();
  }

  interface IntBacked<T extends RegionBasedRegion<T, R>, R extends IntBackedRegion> extends IntBackedRegion, RegionBasedRegion<T, R> {

    @Override
    default T moved(Vec3i relativePos) {
      return newRegion((R) region().moved(relativePos));
    }

    @Override
    default T moved(Vec3 relativePos) {
      return RegionBasedRegion.super.moved(relativePos);
    }

    @Override
    default T expanded(int offset) {
      return newRegion((R) region().expanded(offset));
    }

    @Override
    default T expanded(int offset, Direction.Axis axis) {
      return newRegion((R) region().expanded(offset, axis));
    }

    @Override
    default T expanded(int offset, Direction direction) {
      return newRegion((R) region().expanded(offset, direction));
    }

    @Override
    default T expanded(int offset, Direction.Plane type) {
      return newRegion((R) region().expanded(offset, type));
    }

    @Override
    default T rotated(Rotation blockRotation, Vec3 pivot) {
      return RegionBasedRegion.super.rotated(blockRotation, pivot);
    }

    @Override
    default T mirrored(Direction.Axis axis, Vec3 pivot) {
      return RegionBasedRegion.super.mirrored(axis, pivot);
    }

    @Override
    default T transformed(Function<Vec3, Vec3> transformation) {
      return (T) IntBackedRegion.super.transformed(transformation);
    }

    @Override
    default T transformedInt(Function<Vec3i, Vec3i> transformation) {
      return newRegion((R) region().transformedInt(transformation));
    }

    @Override
    default T expanded(double offset) {
      return RegionBasedRegion.super.expanded(offset);
    }

    @Override
    default T expanded(double offset, Direction.Axis axis) {
      return RegionBasedRegion.super.expanded(offset, axis);
    }

    @Override
    default T expanded(double offset, Direction direction) {
      return RegionBasedRegion.super.expanded(offset, direction);
    }

    @Override
    default T expanded(double offset, Direction.Plane type) {
      return RegionBasedRegion.super.expanded(offset, type);
    }

    @Override
    default boolean contains(Vec3 vec3d) {
      return IntBackedRegion.super.contains(vec3d);
    }

    @Override
    default boolean contains(Vec3i vec3i) {
      return region().contains(vec3i);
    }

    @Override
    default long numberOfBlocksAffected() {
      return region().numberOfBlocksAffected();
    }

    @Override
    @Nullable
    default BoundingBox minContainingBlockBox() {
      return region().minContainingBlockBox();
    }

    @Override
    @Nullable
    default AABB minContainingBox() {
      return region().minContainingBox();
    }

    @Override
    default double volume() {
      return region().volume();
    }
  }
}
