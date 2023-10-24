package pers.solid.ecmd.regionbuilder;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.GeoUtil;

import java.util.List;
import java.util.function.Function;

/**
 * 区域构建器可用于玩家在游戏内通过交互式的操作来建立一个区域。这个区域会在服务器和客户端之间进行同步，因此需要实现与 NBT 之间的转换。
 */
public interface RegionBuilder {
  SimpleCommandExceptionType NOT_COMPLETED = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.region_builder.not_completed"));

  /**
   * 设置第一个点时的操作。有可能是直接设置的特定的点，也有可能是重新开始一个全新的区域。
   */
  void clickFirstPoint(BlockPos point, PlayerEntity player);

  /**
   * 设置第二个点时的操作。有可能是直接设置的特定的点，也有可能是在多个点的列表中增加一个点。
   */
  void clickSecondPoint(BlockPos point, PlayerEntity player);

  List<@NotNull Vec3d> getPoints();

  void readPoints(List<Vec3d> points);

  default void inheritPointsFrom(RegionBuilder source) {
    readPoints(source.getPoints());
  }

  /**
   * 撤销上一个点的操作，仅限于有多个点时的操作。
   */
  default void popLastOperation(PlayerEntity player) {
    throw new UnsupportedOperationException();
  }

  /**
   * 建立起绝对的区域。
   */
  Region buildRegion() throws CommandSyntaxException;

  default void move(@NotNull Vec3i relativePos) {
    move(Vec3d.of(relativePos));
  }

  default void move(@NotNull Vec3d relativePos) {
    transform(vec3d -> vec3d.add(relativePos));
  }

  default void rotate(@NotNull BlockRotation blockRotation, @NotNull Vec3d pivot) {
    transform(vec3d -> GeoUtil.rotate(vec3d, blockRotation, pivot));
  }

  default void mirror(@NotNull Direction.Axis axis, @NotNull Vec3d pivot) {
    transform(vec3d -> GeoUtil.mirror(vec3d, axis, pivot));
  }

  /**
   * 区域向各方向延伸浮点数值后的区域。
   */
  default void expand(double offset) {
    throw new UnsupportedOperationException();
  }

  /**
   * 区域沿指定坐标轴延伸浮点数值后的区域。
   */
  default void expand(double offset, Direction.Axis axis) {
    throw new UnsupportedOperationException();
  }

  /**
   * 区域往指定方向延伸浮点数值后的区域，被延伸的那一侧是沿该方向最远的一侧。
   */
  default void expand(double offset, Direction direction) {
    throw new UnsupportedOperationException();
  }

  default void expand(double offset, Direction.Type type) {
    throw new UnsupportedOperationException();
  }

  void transform(Function<Vec3d, Vec3d> transformation);
}
