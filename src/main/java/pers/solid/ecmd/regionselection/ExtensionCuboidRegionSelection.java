package pers.solid.ecmd.regionselection;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;
import java.util.function.Supplier;

public class ExtensionCuboidRegionSelection extends BlockCuboidRegionSelection {
  @Override
  public Supplier<Text> clickFirstPoint(BlockPos point, PlayerEntity player) {
    first = point;
    second = null;
    resetCalculation();
    return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.argument.region_selection.cuboid.set_first", TextUtil.wrapVector(first).styled(TextUtil.STYLE_FOR_RESULT)), BlockCuboidRegionSelection.notifyStatistics(first, second));
  }

  @Override
  public Supplier<Text> clickSecondPoint(BlockPos point, PlayerEntity player) {
    if (first == null) {
      first = point;
      return () -> (Text.translatable("enhanced_commands.argument.region_selection.cuboid.set_first", TextUtil.wrapVector(first).styled(TextUtil.STYLE_FOR_RESULT)));
    } else if (second == null) {
      second = point;
      resetCalculation();
      return () -> (Text.translatable("enhanced_commands.argument.region_selection.extension.include", TextUtil.wrapVector(point).styled(TextUtil.STYLE_FOR_RESULT)));
    } else {
      final BlockBox blockBox = BlockBox.create(first, second);
      if (blockBox.contains(point)) {
        return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.argument.region_selection.extension.not_infected", TextUtil.wrapVector(point).styled(TextUtil.STYLE_FOR_RESULT)), BlockCuboidRegionSelection.notifyStatistics(first, second));
      }
      final BlockPos.Mutable mutable1 = new BlockPos.Mutable().set(first);
      final BlockPos.Mutable mutable2 = new BlockPos.Mutable().set(second);
      if (point.getX() > blockBox.getMaxX()) {
        (first.getX() > second.getX() ? mutable1 : mutable2).setX(point.getX());
      }
      if (point.getX() < blockBox.getMinX()) {
        (first.getX() < second.getX() ? mutable1 : mutable2).setX(point.getX());
      }
      if (point.getY() > blockBox.getMaxY()) {
        (first.getY() > second.getY() ? mutable1 : mutable2).setY(point.getY());
      }
      if (point.getY() < blockBox.getMinY()) {
        (first.getY() < second.getY() ? mutable1 : mutable2).setY(point.getY());
      }
      if (point.getZ() > blockBox.getMaxZ()) {
        (first.getZ() > second.getZ() ? mutable1 : mutable2).setZ(point.getZ());
      }
      if (point.getZ() < blockBox.getMinZ()) {
        (first.getZ() < second.getZ() ? mutable1 : mutable2).setZ(point.getZ());
      }
      if (!mutable1.equals(first)) {
        first = mutable1.toImmutable();
      }
      if (!mutable2.equals(second)) {
        second = mutable2.toImmutable();
      }
      resetCalculation();
      return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.argument.region_selection.extension.include", TextUtil.wrapVector(point).styled(TextUtil.STYLE_FOR_RESULT)), BlockCuboidRegionSelection.notifyStatistics(first, second));
    }
  }

  @Override
  public void readPoints(List<Vec3d> points) {
    if (!points.isEmpty()) {
      first = BlockPos.ofFloored(points.get(0));
      if (points.size() > 1) {
        second = BlockPos.ofFloored(points.get(points.size() - 1));
      }
      resetCalculation();
    }
  }

  @Override
  public @NotNull RegionSelectionType getSelectionType() {
    return RegionSelectionTypes.EXTENSION;
  }


  @Override
  public @NotNull ExtensionCuboidRegionSelection clone() {
    return (ExtensionCuboidRegionSelection) super.clone();
  }
}
