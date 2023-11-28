package pers.solid.ecmd.regionselection;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.region.*;
import pers.solid.ecmd.util.FunctionParamsParser;
import pers.solid.ecmd.util.GeoUtil;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 区域选择可用于玩家在游戏内通过交互式的操作来建立一个区域。这个区域会在服务器和客户端之间进行同步，因此需要实现与 NBRegionBuilder 之间的转换。
 */
public interface RegionSelection extends RegionBasedRegion<RegionSelection, Region> {
  Text NOT_COMPLETED = Text.translatable("enhanced_commands.argument.region_selection.not_completed");

  /**
   * 设置第一个点时的操作。有可能是直接设置的特定的点，也有可能是重新开始一个全新的区域。
   *
   * @return
   */
  Supplier<Text> clickFirstPoint(BlockPos point, PlayerEntity player);

  /**
   * 设置第二个点时的操作。有可能是直接设置的特定的点，也有可能是在多个点的列表中增加一个点。
   *
   * @return
   */
  Supplier<Text> clickSecondPoint(BlockPos point, PlayerEntity player);

  List<@NotNull Vec3d> getPoints();

  void readPoints(List<Vec3d> points);

  default void inheritPointsFrom(RegionSelection source) {
    readPoints(source.getPoints());
  }

  /**
   * 撤销上一个点的操作，仅限于有多个点时的操作。
   */
  default void popLastOperation(PlayerEntity player) {
    throw new UnsupportedOperationException();
  }


  /**
   * 移动选区自身，并通常返回自身。
   */
  @Contract(mutates = "this")
  default @NotNull RegionSelection moved(@NotNull Vec3i relativePos) {
    return moved(Vec3d.of(relativePos));
  }

  default @NotNull RegionSelection moved(@NotNull Vec3d relativePos) {
    return transformed(vec3d -> vec3d.add(relativePos));
  }

  default @NotNull RegionSelection rotated(@NotNull BlockRotation blockRotation, @NotNull Vec3d pivot) {
    return transformed(vec3d -> GeoUtil.rotate(vec3d, blockRotation, pivot));
  }

  default @NotNull RegionSelection mirrored(@NotNull Direction.Axis axis, @NotNull Vec3d pivot) {
    return transformed(vec3d -> GeoUtil.mirror(vec3d, axis, pivot));
  }

  default @NotNull RegionSelection expanded(double offset) {
    throw new UnsupportedOperationException();
  }

  /**
   * 区域沿指定坐标轴延伸浮点数值后的区域。
   *
   * @return
   */
  default @NotNull RegionSelection expanded(double offset, Direction.Axis axis) {
    throw new UnsupportedOperationException();
  }

  /**
   * 区域往指定方向延伸浮点数值后的区域，被延伸的那一侧是沿该方向最远的一侧。
   *
   * @return
   */
  default @NotNull RegionSelection expanded(double offset, Direction direction) {
    throw new UnsupportedOperationException();
  }

  default @NotNull RegionSelection expanded(double offset, Direction.Type type) {
    throw new UnsupportedOperationException();
  }

  /**
   * 对区域对象自身进行修改并返回自身。
   */
  RegionSelection transformed(Function<Vec3d, Vec3d> transformation);

  /**
   * 转换为具体的 Region 对象，该对象通常不应该是 RegionBuilder 对象。一般来说，它应该是缓存在对象的字段中的，如果自身有修改，则该字段清除，下次调用时再重新计算。
   */
  @Override
  Region region();

  RegionSelection clone();

  @Override
  default RegionSelection newRegion(Region region) {
    return this;
  }

  @Override
  @NotNull
  default String asString() {
    return region().asString();
  }

  @Override
  @NotNull
  default Type getType() {
    return RegionTypes.BUILDER;
  }

  @NotNull
  RegionSelectionType getBuilderType();

  enum Type implements RegionType<RegionSelection> {
    INSTANCE;

    @Override
    public FunctionParamsParser<RegionArgument> functionParamsParser() {
      return null;
    }
  }
}
