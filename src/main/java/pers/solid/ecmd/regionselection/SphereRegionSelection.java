package pers.solid.ecmd.regionselection;

import net.minecraft.command.CommandException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.region.SphereRegion;
import pers.solid.ecmd.util.NbtUtil;
import pers.solid.ecmd.util.Styles;
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
    if (center != null && radiusTarget != null) {
      radius = center.distanceTo(radiusTarget);
    } else {
      radius = 0;
    }
  }

  @Override
  public Supplier<Text> clickFirstPoint(BlockPos point, PlayerEntity player) {
    center = point.toCenterPos();
    resetCalculation();
    return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.region_selection.sphere.set_center", TextUtil.wrapVector(point).styled(Styles.RESULT)), notifySphereStatistics());
  }

  @Override
  public Supplier<Text> clickSecondPoint(BlockPos point, PlayerEntity player) {
    radiusTarget = point.toCenterPos();
    resetCalculation();
    return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.region_selection.sphere.set_radius", TextUtil.wrapVector(point).styled(Styles.RESULT)), notifySphereStatistics());
  }

  public Text notifySphereStatistics() {
    if (center != null && radiusTarget != null) {
      updateRadius();
      return (Text.translatable("enhanced_commands.region_selection.sphere.statistics", TextUtil.literal(radius).styled(Styles.RESULT)).formatted(Formatting.GRAY));
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
  public @NotNull SphereRegionSelection transformed(Function<Vec3d, Vec3d> transformation) {
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
  public @NotNull RegionSelectionType getSelectionType() {
    return RegionSelectionTypes.SPHERE;
  }

  @Override
  public void fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
    center = nbtCompound.contains("center", NbtElement.COMPOUND_TYPE) ? NbtUtil.toVec3d(nbtCompound.getCompound("center")) : null;
    radiusTarget = nbtCompound.contains("radius_target", NbtElement.COMPOUND_TYPE) ? NbtUtil.toVec3d(nbtCompound.getCompound("radius_target")) : null;
    updateRadius();
  }

  @Override
  public SphereRegionSelection clone() {
    return (SphereRegionSelection) super.clone();
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.put("center", NbtUtil.fromVec3d(center));
    nbtCompound.put("radius_target", NbtUtil.fromVec3d(radiusTarget));
  }
}
