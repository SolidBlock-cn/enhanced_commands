package pers.solid.ecmd.regionselection;

import net.minecraft.command.CommandException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.region.BlockCuboidRegion;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class BlockCuboidRegionSelection extends AbstractRegionSelection<BlockCuboidRegion> implements IntBackedRegionSelection, Cloneable {
  public Vec3i first;
  public Vec3i second;

  @Override
  public Supplier<Text> clickFirstPoint(BlockPos point, PlayerEntity player) {
    first = point;
    resetCalculation();
    return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.argument.region_selection.cuboid.set_first", TextUtil.wrapVector(first).styled(TextUtil.STYLE_FOR_RESULT)), notifyStatistics(first, second));
  }

  @Override
  public Supplier<Text> clickSecondPoint(BlockPos point, PlayerEntity player) {
    second = point;
    resetCalculation();
    return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.argument.region_selection.cuboid.set_second", TextUtil.wrapVector(second).styled(TextUtil.STYLE_FOR_RESULT)), notifyStatistics(first, second));
  }

  public static Text notifyStatistics(@Nullable Vec3i first, @Nullable Vec3i second) {
    if (first != null && second != null) {
      final Vec3i subtract = first.subtract(second);
      final int dx = Math.abs(subtract.getX()) + 1;
      final int dy = Math.abs(subtract.getY()) + 1;
      final int dz = Math.abs(subtract.getZ()) + 1;
      return (Text.translatable("enhanced_commands.argument.region_selection.cuboid.statistics", Text.literal(dx + "×" + dy + "×" + dz).styled(TextUtil.STYLE_FOR_RESULT), TextUtil.literal(dx * dy * dz).styled(TextUtil.STYLE_FOR_RESULT)).formatted(Formatting.GRAY));
    } else {
      return null;
    }
  }

  @Override
  public List<@NotNull Vec3d> getPoints() {
    return Stream.of(first, second).filter(Objects::nonNull).map(Vec3d::ofCenter).toList();
  }

  @Override
  public void readPoints(List<Vec3d> points) {
    if (!points.isEmpty()) {
      first = BlockPos.ofFloored(points.get(0));
    }
    if (points.size() > 1) {
      second = BlockPos.ofFloored(points.get(points.size() - 1));
    }
    resetCalculation();
  }

  @Override
  public BlockCuboidRegion buildRegion() {
    if (first == null || second == null) {
      throw new CommandException(NOT_COMPLETED);
    } else {
      return new BlockCuboidRegion(first, second);
    }
  }

  @Override
  public @NotNull RegionSelection expanded(int offset) {
    int x1 = first.getX();
    int y1 = first.getY();
    int z1 = first.getZ();
    int x2 = second.getX();
    int y2 = second.getY();
    int z2 = second.getZ();
    final Vec3i pos1Offset = new Vec3i(x1 > x2 ? offset : -offset, y1 > y2 ? offset : -offset, z1 > z2 ? offset : -offset);
    first = first.add(pos1Offset);
    second = second.subtract(pos1Offset);
    resetCalculation();
    return this;
  }

  @Override
  public @NotNull RegionSelection expanded(int offset, Direction direction) {
    final boolean negative = direction.getDirection() == Direction.AxisDirection.NEGATIVE;
    final Direction.Axis axis = direction.getAxis();
    final int firstCoordination = first.getComponentAlongAxis(axis);
    final int secondCoordination = second.getComponentAlongAxis(axis);

    if (!negative) {
      offset = -offset;
    }
    if (firstCoordination > secondCoordination == negative) {
      switch (axis) {
        case X -> first = first.add(offset, 0, 0);
        case Y -> first = first.add(0, offset, 0);
        case Z -> first = first.add(0, 0, offset);
      }
    } else {
      switch (axis) {
        case X -> second = second.add(offset, 0, 0);
        case Y -> second = second.add(0, offset, 0);
        case Z -> second = second.add(0, 0, offset);
      }
    }
    resetCalculation();
    return this;
  }

  @Override
  public RegionSelection expanded(int offset, Direction.Axis axis) {
    final Vec3i pos1Offset = new Vec3i(first.getX() > second.getX() ? offset : -offset, first.getY() > second.getY() ? offset : -offset, first.getZ() > second.getZ() ? offset : -offset);

    first = first.add(pos1Offset);
    second = second.subtract(pos1Offset);
    resetCalculation();
    return this;
  }

  @Override
  public Region expanded(int offset, Direction.Type type) {
    final Vec3i pos1Offset = switch (type) {
      case VERTICAL -> new Vec3i(0, first.getY() > second.getY() ? offset : -offset, 0);
      case HORIZONTAL ->
          new Vec3i(first.getX() > second.getX() ? offset : -offset, 0, first.getZ() > second.getZ() ? offset : -offset);
    };
    first = first.add(pos1Offset);
    second = second.subtract(pos1Offset);
    resetCalculation();
    return this;
  }

  @Override
  public @NotNull RegionSelectionType getBuilderType() {
    return RegionSelectionTypes.CUBOID;
  }

  @Override
  public @NotNull IntBackedRegionSelection transformedInt(Function<Vec3i, Vec3i> transformation) {
    first = transformation.apply(first);
    second = transformation.apply(second);
    resetCalculation();
    return this;
  }

  @Override
  public @NotNull IntBackedRegionSelection clone() {
    return (IntBackedRegionSelection) super.clone();
  }
}
