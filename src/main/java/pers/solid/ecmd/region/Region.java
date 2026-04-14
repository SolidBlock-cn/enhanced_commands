package pers.solid.ecmd.region;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.PositionProvider;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 区域是指一系列坐标的抽象集合，每个区域需要能够遍历区域中的方块坐标，并且能够判断精确坐标或方块坐标是否在区域内。区域使用的坐标是精确的，不一定是方块坐标，如果所有的操作都是基于方块的，则可以使用 {@link IntBackedRegion}。
 */
@Unmodifiable
public interface Region extends Iterable<BlockPos>, ExpressionConvertible {
  ResourceKey<Registry<Region>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("region"));
  Codec<Region> CODEC = RegionType.REGISTRY.byNameCodec().dispatch(Region::getType, RegionType::getCodec);

  static Region getCached(RegionProvider<?> regionProvider, PositionProvider positionProvider) {
    return CacheStorage.cache.getUnchecked(regionProvider).getUnchecked(positionProvider);
  }

  /**
   * 判断方块坐标是否在该区域内。其默认的实现方式是判断方块坐标的中心位置。
   */
  @Contract(pure = true)
  default boolean contains(Vec3i vec3i) {
    return contains(Vec3.atCenterOf(vec3i));
  }

  /**
   * 判断精确坐标是否在该区域内。
   */
  @Contract(pure = true)
  boolean contains(Vec3 vec3d);

  /**
   * 返回该区域内的所有{@linkplain BlockPos 方块坐标}的{@linkplain Iterator 迭代器}。<strong>注意：</strong>返回的 {@link BlockPos} 可能是 {@linkplain BlockPos.MutableBlockPos 可变的}，有可能是同一个对象但是一边修改一边返回。如果需要将返回的方块坐标存储到集合中，需要调用 {@link BlockPos.MutableBlockPos#immutable()} 以避免问题。
   */
  @Override
  Iterator<BlockPos> iterator();

  /**
   * 返回该区域内的所有方块坐标的{@linkplain Stream 流}。<strong>注意：</strong>返回的 {@link BlockPos} 可能是 {@linkplain BlockPos.MutableBlockPos 可变的}，参见 {@link #iterator()}。
   */
  default Stream<BlockPos> stream() {
    return Streams.stream(this);
  }

  /**
   * 该区域沿指定的整数向量移动后的区域。默认情况下会将这个整数向量转换为浮点向量，但特定情况下可以修改此方法以避免使用浮点数。
   */
  default Region moved(Vec3i relativePos) {
    return moved(Vec3.atLowerCornerOf(relativePos));
  }

  /**
   * 该区域沿指定的浮点数向量移动后的区域。
   */
  default Region moved(Vec3 relativePos) {
    return transformed(vec3d -> vec3d.add(relativePos));
  }

  /**
   * 区域旋转后的区域。
   *
   * @implSpec 此区域内的所有坐标在旋转后都应该是旋转后的区域内的所有坐标，但是不需要确保旋转后的迭代顺序与之前的一致。
   */
  default Region rotated(Rotation blockRotation, Vec3 pivot) {
    return transformed(vec3d -> GeoUtil.rotate(vec3d, blockRotation, pivot));
  }

  /**
   * 区域翻转后的区域。
   *
   * @implSpec 此区域内的所有坐标在翻转后都应该是翻转后的区域内的所有坐标，但是不需要确保翻转后的迭代顺序与之前的一致。
   */
  default Region mirrored(Direction.Axis axis, Vec3 pivot) {
    return transformed(vec3d -> GeoUtil.mirror(vec3d, axis, pivot));
  }

  Region transformed(Function<Vec3, Vec3> transformation);

  /**
   * 区域向各方向延伸浮点数值后的区域。
   */
  default Region expanded(double offset) {
    throw new UnsupportedOperationException();
  }

  /**
   * 区域沿指定坐标轴延伸浮点数值后的区域。
   */
  default Region expanded(double offset, Direction.Axis axis) {
    throw new UnsupportedOperationException();
  }

  /**
   * 区域往指定方向延伸浮点数值后的区域，被延伸的那一侧是沿该方向最远的一侧。
   */
  default Region expanded(double offset, Direction direction) {
    throw new UnsupportedOperationException();
  }

  /**
   * 区域往水平或者竖直方向上延伸浮点数值后的区域。
   */
  default Region expanded(double offset, Direction.Plane type) {
    throw new UnsupportedOperationException();
  }

  RegionType<?> getType();

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
  String asString();

  /**
   * 包含该区域内所有坐标的最小长方体区域。
   */
  @Nullable
  AABB minContainingBox();

  /**
   * 包含该区域内所有坐标的最小长方体方块区域，用于判断该区域内是否在坐标<em>可能</em>不在已加载的区块内。
   */
  default @Nullable BoundingBox minContainingBlockBox() {
    final AABB minContainingBox = minContainingBox();
    return minContainingBox == null ? null : new BoundingBox(Mth.floor(minContainingBox.minX), Mth.floor(minContainingBox.minY), Mth.floor(minContainingBox.minZ), Mth.floor(minContainingBox.maxX), Mth.floor(minContainingBox.maxY), Mth.floor(minContainingBox.maxZ));
  }

  class CacheStorage {
    private static final LoadingCache<RegionProvider<?>, LoadingCache<PositionProvider, Region>> cache = CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(regionArgument -> CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(regionArgument::toAbsoluteRegion))));
  }
}
