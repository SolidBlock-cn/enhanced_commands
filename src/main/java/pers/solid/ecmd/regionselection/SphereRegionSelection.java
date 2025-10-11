package pers.solid.ecmd.regionselection;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.region.SphereRegion;
import pers.solid.ecmd.render.RegionRendering;
import pers.solid.ecmd.render.VertexUtil;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SphereRegionSelection extends AbstractRegionSelection<SphereRegion> implements RegionSelection, Cloneable {
  public static final MapCodec<SphereRegionSelection> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Vec3d.CODEC.optionalFieldOf("center").forGetter(s -> Optional.ofNullable(s.center)),
      Vec3d.CODEC.optionalFieldOf("radius_target").forGetter(s -> Optional.ofNullable(s.radiusTarget))
  ).apply(i, SphereRegionSelection::fromOptional));

  public @Nullable Vec3d center;
  public @Nullable Vec3d radiusTarget;
  public double radius;
  protected List<Vec3d> circle1points = null;
  protected List<Vec3d> circle2points = null;
  protected List<Vec3d> circle3points = null;
  protected List<List<Vec3d>> circlePointsList = null;

  private static SphereRegionSelection fromOptional(Optional<Vec3d> center, Optional<Vec3d> radiusTarget) {
    final SphereRegionSelection s = new SphereRegionSelection();
    s.center = center.orElse(null);
    s.radiusTarget = radiusTarget.orElse(null);
    s.updateRadius();
    return s;
  }

  public void updateRadius() {
    if (center != null && radiusTarget != null) {
      radius = center.distanceTo(radiusTarget);
    } else {
      radius = 0;
    }
  }

  @Override
  public Supplier<Text> clickFirstPoint(Vec3d point, PlayerEntity player) {
    center = point;
    resetCalculation();
    return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.region_selection.sphere.set_center", TextUtil.wrapVector(point).styled(Styles.RESULT)), notifySphereStatistics());
  }

  @Override
  public Supplier<Text> clickSecondPoint(Vec3d point, PlayerEntity player) {
    radiusTarget = point;
    resetCalculation();
    return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.region_selection.sphere.set_radius", TextUtil.wrapVector(point).styled(Styles.RESULT)), notifySphereStatistics());
  }

  public Text notifySphereStatistics() {
    if (center != null && radiusTarget != null) {
      updateRadius();
      return Text.translatable("enhanced_commands.region_selection.sphere.statistics", Text.literal(StringUtil.nf.format(radius)).styled(Styles.RESULT)).formatted(Formatting.GRAY);
    } else {
      return null;
    }
  }

  @Override
  public List<@NotNull Vec3d> getPoints() {
    final Stream<@NotNull Vec3d> stream = Stream.of(center, radiusTarget).filter(Objects::nonNull);
    return stream.toList();
  }

  @Override
  public void readPoints(List<Vec3d> points) {
    if (!points.isEmpty()) {
      center = points.get(0);
      if (points.size() > 1) {
        radiusTarget = points.get(points.size() - 1);
      }
    }
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
  public @NotNull SphereRegionSelection transformed(Function<Vec3d, Vec3d> transformation) throws CommandSyntaxException {
    if (center == null || radiusTarget == null) {
      throw NOT_COMPLETED.create();
    }
    final Vec3d oldCenter = center;
    center = transformation.apply(oldCenter);
    radiusTarget = radiusTarget.add(center.subtract(oldCenter));
    resetCalculation();
    return this;
  }

  @Override
  public @NotNull SphereRegionSelection expanded(double offset) throws CommandSyntaxException {
    if (center == null || radiusTarget == null) {
      throw NOT_COMPLETED.create();
    }
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
  public @NotNull RegionSelectionType getType() {
    return RegionSelectionTypes.SPHERE;
  }

  @Override
  public SphereRegionSelection clone() {
    return (SphereRegionSelection) super.clone();
  }

  @Override
  public void readPacket(PacketByteBuf buf) {
    center = buf.readBoolean() ? buf.readVec3d() : null;
    radiusTarget = buf.readBoolean() ? buf.readVec3d() : null;
    resetCalculation();
  }

  @Override
  public void writePacket(PacketByteBuf buf) {
    buf.writeBoolean(center != null);
    if (center != null) {
      buf.writeVec3d(center);
    }
    buf.writeBoolean(radiusTarget != null);
    if (radiusTarget != null) {
      buf.writeVec3d(radiusTarget);
    }
  }

  @Override
  public void resetCalculation() {
    super.resetCalculation();
    updateRadius();
    circle1points = null;
    circle2points = null;
    circle3points = null;
    circlePointsList = null;
  }

  protected void updateCirclePoints() {
    if (center == null) {
      circlePointsList = List.of();
      return;
    }

    final int pointsNum = Math.min(MathHelper.ceil(Math.PI * radius * 4) * 4, 2048);
    final double angleInterval = Math.max(2 * Math.PI / pointsNum, 1 / 2048d);

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
    final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RegionRendering.regionRenderLayer);

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
      VertexUtil.drawLineConnecting(matrices, vertexConsumer, center.x - cameraX, center.y - cameraY, center.z - cameraZ, radiusTarget.x - cameraX, radiusTarget.y - cameraY, radiusTarget.z - cameraZ, ColorHelper.Argb.fromFloats(0.9f, 0.6f, 0.9f, 1), ColorHelper.Argb.fromFloats(0.9f, 0.6f, 1f, 0.8f));
    }

    // 渲染圆
    if (circlePointsList == null) {
      updateCirclePoints();
    }
    for (var list : circlePointsList) {
      for (int i = 0; i < list.size(); i++) {
        final Vec3d current = list.get(i);
        final Vec3d next = list.get(i + 1 < list.size() ? i + 1 : 0);
        VertexUtil.drawLineConnecting(matrices, vertexConsumer, current.x - cameraX, current.y - cameraY, current.z - cameraZ, next.x - cameraX, next.y - cameraY, next.z - cameraZ, 0xeee8ffd0);
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
        int color;
        if (direction == Direction.EAST) {
          color = ColorHelper.Argb.fromFloats(1f, 0.9f, 0.5f, 0.5f);
        } else if (direction == Direction.SOUTH) {
          color = ColorHelper.Argb.fromFloats(1f, 0.5f, 0.9f, 0.5f);
        } else if (direction == Direction.UP) {
          color = ColorHelper.Argb.fromFloats(1f, 0.5f, 0.5f, 0.9f);
        } else {
          color = ColorHelper.Argb.fromFloats(1f, 0.9f, 0.9f, 0.9f);
        }
        VertexUtil.drawLineConnecting(matrices, vertexConsumer, center.x - cameraX, center.y - cameraY, center.z - cameraZ, center.x - cameraX + radius * direction.getOffsetX(), center.y - cameraY + radius * direction.getOffsetY(), center.z - cameraZ + radius * direction.getOffsetZ(), color);
      }
    }
  }

}
