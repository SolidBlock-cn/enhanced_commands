package pers.solid.ecmd.region;

import com.google.common.collect.Streams;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * 表示一个区域，每个区域需要包含多个方块坐标，并且能够判断坐标是否在区域内。区域使用的坐标是精确的，不一定是方块坐标，如果所有的操作都是基于方块的，则可以使用 {@link IntBackedRegion}。
 */
@Unmodifiable
public interface Region extends Iterable<BlockPos>, ExpressionConvertible, RegionArgument<Region> {
  /**
   * 判断方块坐标是否在该区域内。其默认的实现方式是判断方块坐标的中心位置。
   */
  @Contract(pure = true)
  default boolean contains(@NotNull Vec3i vec3i) {
    return contains(Vec3d.ofCenter(vec3i));
  }

  /**
   * 判断精确坐标是否在该区域内。
   */
  @Contract(pure = true)
  boolean contains(@NotNull Vec3d vec3d);

  /**
   * 返回该区域内的所有{@linkplain BlockPos 方块坐标}的{@linkplain Iterator 迭代器}。<strong>注意：</strong>返回的 {@link BlockPos} 可能是 {@linkplain BlockPos.Mutable 可变的}，有可能是同一个对象但是一边修改一边返回。如果需要将返回的方块坐标存储到集合中，需要调用 {@link BlockPos.Mutable#toImmutable()} 以避免问题。
   */
  @NotNull
  @Override
  Iterator<BlockPos> iterator();

  /**
   * 返回该区域内的所有方块坐标的{@linkplain Stream 流}。<strong>注意：</strong>返回的 {@link BlockPos} 可能是 {@linkplain BlockPos.Mutable 可变的}，参见 {@link #iterator()}。
   */
  default Stream<BlockPos> stream() {
    return Streams.stream(this);
  }

  /**
   * 该区域在指定方向上移动整数格后的区域。
   */
  @NotNull
  default Region moved(int count, @NotNull Direction direction) {
    return moved(direction.getVector().multiply(count));
  }

  /**
   * 该区域在指定方向上移动浮点数格后的区域。
   */
  @NotNull
  default Region moved(double count, @NotNull Direction direction) {
    return moved(Vec3d.of(direction.getVector()).multiply(count));
  }

  /**
   * 该区域沿指定的整数向量移动后的区域。默认情况下会将这个整数向量转换为浮点向量，但特定情况下可以修改此方法以避免使用浮点数。
   */
  @NotNull
  default Region moved(@NotNull Vec3i relativePos) {
    return moved(Vec3d.of(relativePos));
  }

  /**
   * 该区域沿指定的浮点数向量移动后的区域。
   */
  @NotNull
  Region moved(@NotNull Vec3d relativePos);

  /**
   * 区域旋转后的区域。
   *
   * @implSpec 此区域内的所有坐标在旋转后都应该是旋转后的区域内的所有坐标，但是不需要确保旋转后的迭代顺序与之前的一致。
   */
  @NotNull
  Region rotated(@NotNull Vec3d pivot, @NotNull BlockRotation blockRotation);

  /**
   * 区域翻转后的区域。
   *
   * @implSpec 此区域内的所有坐标在翻转后都应该是翻转后的区域内的所有坐标，但是不需要确保翻转后的迭代顺序与之前的一致。
   */
  @NotNull
  Region mirrored(@NotNull Vec3d pivot, @NotNull Direction.Axis axis);

  /**
   * 区域向各方向延伸浮点数值后的区域。
   */
  @NotNull
  default Region expanded(double offset) {
    throw new UnsupportedOperationException();
  }

  /**
   * 区域沿指定坐标轴延伸浮点数值后的区域。
   */
  @NotNull
  default Region expanded(double offset, Direction.Axis axis) {
    throw new UnsupportedOperationException();
  }

  /**
   * 区域往指定方向延伸浮点数值后的区域，被延伸的那一侧是沿该方向最远的一侧。
   */
  @NotNull
  default Region expanded(double offset, Direction direction) {
    throw new UnsupportedOperationException();
  }

  @NotNull RegionType<?> getType();

  /**
   * 此区域的体积，通常通过几何公式算出，用于 {@link #numberOfBlocksAffected()}。但是部分类型的区域无法进行计算。
   */
  @Contract(pure = true)
  double volume();

  /**
   * <em>估算</em>可能受影响的最多的方块数量，用于决定在指定命令时是否要使用分段执行以避免卡顿。
   */
  @Contract(pure = true)
  default long numberOfBlocksAffected() {
    return Math.round(volume());
  }

  @Override
  @NotNull String asString();

  /**
   * 包含该区域内所有坐标的最小长方体区域。
   */
  @Nullable Box minContainingBox();

  /**
   * 包含该区域内所有坐标的最小长方体方块区域，用于判断该区域内是否在坐标<em>可能</em>不在已加载的区块内。
   */
  default @Nullable BlockBox maxContainingBlockBox() {
    final Box maxContainingBox = minContainingBox();
    return maxContainingBox == null ? null : new BlockBox(MathHelper.floor(maxContainingBox.minX), MathHelper.floor(maxContainingBox.minY), MathHelper.floor(maxContainingBox.minZ), MathHelper.floor(maxContainingBox.maxX), MathHelper.floor(maxContainingBox.maxY), MathHelper.floor(maxContainingBox.maxZ));
  }

  @Override
  default Region toAbsoluteRegion(ServerCommandSource source) {
    return this;
  }
}
