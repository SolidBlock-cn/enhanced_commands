package pers.solid.ecmd.regionselection;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.region.SphereRegion;
import pers.solid.ecmd.render.RegionRendering;
import pers.solid.ecmd.render.VertexUtil;
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
      return Text.translatable("enhanced_commands.region_selection.sphere.statistics", TextUtil.literal(radius).styled(Styles.RESULT)).formatted(Formatting.GRAY);
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
  public SphereRegion buildRegion() throws CommandSyntaxException {
    if (center == null || radiusTarget == null) {
      throw NOT_COMPLETED.create();
    } else {
      return new SphereRegion(radius, center);
    }
  }

  @Override
  public @NotNull SphereRegionSelection transformed(Function<Vec3d, Vec3d> transformation) {
    final Vec3d oldCenter = center;
    center = transformation.apply(oldCenter);
    radiusTarget = radiusTarget.add(center.subtract(oldCenter));
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

  protected List<Vec3d> circle1points = null;
  protected List<Vec3d> circle2points = null;
  protected List<Vec3d> circle3points = null;
  protected List<List<Vec3d>> circlePointsList = null;

  @Override
  public void resetCalculation() {
    super.resetCalculation();
    circle1points = null;
    circle2points = null;
    circle3points = null;
    circlePointsList = null;
  }

  protected void updateCirclePoints() {
    if (center == null) {
      return;
    }

    final int pointsNum = MathHelper.ceil(Math.PI * radius * 4) * 4;
    final double angleInterval = 2 * Math.PI / pointsNum;

    final ImmutableList.Builder<Vec3d> builder1 = new ImmutableList.Builder<>();
    final ImmutableList.Builder<Vec3d> builder2 = new ImmutableList.Builder<>();
    final ImmutableList.Builder<Vec3d> builder3 = new ImmutableList.Builder<>();

    for (double angle = 0; angle < 2 * Math.PI; angle += angleInterval) {
      final double cos = Math.cos(angle);
      final double sin = Math.sin(angle);
      final double r_cos = radius * cos;
      final double r_sin = radius * sin;
      builder1.add(new Vec3d(center.x + r_cos, center.y + r_sin, center.z));
      builder2.add(new Vec3d(center.x, center.y + r_cos, center.z + r_sin));
      builder3.add(new Vec3d(center.x + r_sin, center.y, center.z + r_cos));
    }

    circle1points = builder1.build();
    circle2points = builder2.build();
    circle3points = builder3.build();
    circlePointsList = List.of(circle1points, circle2points, circle3points);
  }

  @Environment(EnvType.CLIENT)
  @Override
  public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d cameraPos) {
    final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RegionRendering.reagionRenderLayer);

    final double cameraX = cameraPos.x;
    final double cameraY = cameraPos.y;
    final double cameraZ = cameraPos.z;

    // 构建未完成，闪烁
    if ((center == null) != (radiusTarget == null)) {
      // 构建未完成时，闪烁
      final long measuringTimeMs = Util.getMeasuringTimeMs();
      if (measuringTimeMs % 600 > 300) {
        return;
      }
    }

    // 渲染半径向量
    if (center != null && radiusTarget != null) {
      VertexUtil.drawLineConnecting(matrices, vertexConsumer, center.x - cameraX, center.y - cameraY, center.z - cameraZ, radiusTarget.x - cameraX, radiusTarget.y - cameraY, radiusTarget.z - cameraZ, ColorHelper.fromFloats(0.9f, 0.6f, 0.9f, 1), ColorHelper.fromFloats(0.9f, 0.6f, 1f, 0.8f));
    }

    // 渲染圆
    if (circlePointsList == null) {
      updateCirclePoints();
    }
    for (var list : circlePointsList) {
      for (int i = 0; i < list.size(); i++) {
        final Vec3d current = list.get(i);
        final Vec3d next = list.get(i + 1 < list.size() ? i + 1 : 0);
        VertexUtil.drawLineConnecting(matrices, vertexConsumer, current.x - cameraX, current.y - cameraY, current.z - cameraZ, next.x - cameraX, next.y - cameraY, next.z - cameraZ, 0xb0c0c0c0);
      }
    }

    // 渲染圆心
    if (center != null) {
      VertexUtil.drawUnitBoxCentered(matrices, vertexConsumer, center.x - cameraX, center.y - cameraY, center.z - cameraZ, 0.2f, 0.8f, 1f, 0.9f);
    }

    // 渲染半径终点
    if (radiusTarget != null) {
      VertexUtil.drawUnitBoxCentered(matrices, vertexConsumer, radiusTarget.x - cameraX, radiusTarget.y - cameraY, radiusTarget.z - cameraZ, 0.2f, 1f, 0.8f, 0.9f);
    }

    // 渲染半径
    if (center != null) {
      for (Direction direction : Direction.values()) {
        int color1, color2;
        if (direction == Direction.EAST) {
          color1 = ColorHelper.fromFloats(1f, 0.9f, 0.5f, 0.5f);
          color2 = ColorHelper.fromFloats(0.8f, 0.75f, 0.5f, 0.5f);
        } else if (direction == Direction.SOUTH) {
          color1 = ColorHelper.fromFloats(1f, 0.5f, 0.9f, 0.5f);
          color2 = ColorHelper.fromFloats(0.8f, 0.5f, 0.75f, 0.5f);
        } else if (direction == Direction.UP) {
          color1 = ColorHelper.fromFloats(1f, 0.5f, 0.5f, 0.9f);
          color2 = ColorHelper.fromFloats(0.8f, 0.5f, 0.5f, 0.75f);
        } else {
          color1 = ColorHelper.fromFloats(1f, 0.9f, 0.9f, 0.9f);
          color2 = ColorHelper.fromFloats(0.8f, 0.75f, 0.75f, 0.75f);
        }
        VertexUtil.drawLineConnecting(matrices, vertexConsumer, center.x - cameraX, center.y - cameraY, center.z - cameraZ, center.x - cameraX + radius * direction.getOffsetX(), center.y - cameraY + radius * direction.getOffsetY(), center.z - cameraZ + radius * direction.getOffsetZ(), color1, color2);
      }
    }
  }
}
