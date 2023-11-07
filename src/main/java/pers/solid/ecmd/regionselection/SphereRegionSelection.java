package pers.solid.ecmd.regionselection;

import net.minecraft.command.CommandException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.region.SphereRegion;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SphereRegionSelection extends AbstractRegionSelection<SphereRegion> implements RegionSelection, Cloneable {
  public Vec3d center;
  public Vec3d radiusTarget;
  public double radius;

  public void updateRadius() {
    radius = center.distanceTo(radiusTarget);
  }

  @Override
  public Supplier<Text> clickFirstPoint(BlockPos point, PlayerEntity player) {
    center = point.toCenterPos();
    resetCalculation();
    return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.argument.region_selection.sphere.set_center", TextUtil.wrapVector(point).styled(TextUtil.STYLE_FOR_RESULT)), notifySphereStatistics());
  }

  @Override
  public Supplier<Text> clickSecondPoint(BlockPos point, PlayerEntity player) {
    radiusTarget = point.toCenterPos();
    resetCalculation();
    return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.argument.region_selection.sphere.set_radius", TextUtil.wrapVector(point).styled(TextUtil.STYLE_FOR_RESULT)), notifySphereStatistics());
  }

  public Text notifySphereStatistics() {
    if (center != null && radiusTarget != null) {
      updateRadius();
      return (Text.translatable("enhanced_commands.argument.region_selection.sphere.statistics", TextUtil.literal(radius).styled(TextUtil.STYLE_FOR_RESULT)).formatted(Formatting.GRAY));
    } else {
      return null;
    }
  }

  @Override
  public List<@NotNull Vec3d> getPoints() {
    return Stream.of(center, radiusTarget).filter(Objects::nonNull).toList();
  }

  @Override
  public void readPoints(List<Vec3d> points) {
    if (!points.isEmpty()) {
      center = points.get(0);
      if (points.size() > 1) {
        radiusTarget = points.get(points.size() - 1);
      }
    }
    updateRadius();
    resetCalculation();
  }

  @Override
  public SphereRegion buildRegion() {
    if (center == null || radiusTarget == null) {
      throw new CommandException(NOT_COMPLETED);
    } else {
      return new SphereRegion(radius, center);
    }
  }

  @Override
  public SphereRegionSelection transformed(Function<Vec3d, Vec3d> transformation) {
    center = transformation.apply(center);
    updateRadius();
    resetCalculation();
    return this;
  }

  @Override
  public @NotNull SphereRegionSelection expanded(double offset) {
    final Vec3d radiusVector = radiusTarget.subtract(center);
    final Vec3d newRadiusVector = radiusVector.multiply(1 + offset / radiusVector.length());
    radiusTarget = center.add(newRadiusVector);
    radius += offset;
    resetCalculation();
    return this;
  }

  @Override
  public @NotNull SphereRegionSelection expanded(double offset, Direction direction) {
    throw new UnsupportedOperationException(SphereRegion.EXPAND_FAILED.create());
  }

  @Override
  public @NotNull SphereRegionSelection expanded(double offset, Direction.Type type) {
    throw new UnsupportedOperationException(SphereRegion.EXPAND_FAILED.create());
  }

  @Override
  public @NotNull SphereRegionSelection expanded(double offset, Direction.Axis axis) {
    throw new UnsupportedOperationException(SphereRegion.EXPAND_FAILED.create());
  }

  @Override
  public @NotNull RegionSelectionType getBuilderType() {
    return RegionSelectionTypes.SPHERE;
  }

  @Override
  public SphereRegionSelection clone() {
    return (SphereRegionSelection) super.clone();
  }
}
