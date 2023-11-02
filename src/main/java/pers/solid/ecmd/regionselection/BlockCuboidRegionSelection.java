package pers.solid.ecmd.regionselection;

import net.minecraft.command.CommandException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.region.BlockCuboidRegion;
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
