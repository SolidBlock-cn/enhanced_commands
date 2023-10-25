package pers.solid.ecmd.regionbuilder;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.SphereRegion;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public class SphereRegionBuilder implements RegionBuilder, Cloneable {
  public Vec3d center;
  public Vec3d radiusTarget;
  public double radius;

  public void updateRadius() {
    radius = center.distanceTo(radiusTarget);
  }

  @Override
  public void clickFirstPoint(BlockPos point, PlayerEntity player) {
    center = point.toCenterPos();
    player.sendMessage(Text.translatable("enhancedCommands.argument.region_builder.sphere.set_center", TextUtil.wrapBlockPos(point).styled(TextUtil.STYLE_FOR_RESULT)));
    notifySphereStatistics(player);
  }

  @Override
  public void clickSecondPoint(BlockPos point, PlayerEntity player) {
    radiusTarget = point.toCenterPos();
    player.sendMessage(Text.translatable("enhancedCommands.argument.region_builder.sphere.set_radius", TextUtil.wrapBlockPos(point).styled(TextUtil.STYLE_FOR_RESULT)));
    notifySphereStatistics(player);
  }

  public void notifySphereStatistics(PlayerEntity player) {
    if (center != null && radiusTarget != null) {
      updateRadius();
      player.sendMessage(Text.translatable("enhancedCommands.argument.region_builder.sphere.statistics", TextUtil.literal(radius).styled(TextUtil.STYLE_FOR_RESULT)).formatted(Formatting.GRAY));
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
  }

  @Override
  public Region buildRegion() throws CommandSyntaxException {
    if (center == null || radiusTarget == null) {
      throw NOT_COMPLETED.create();
    } else {
      return new SphereRegion(radius, center);
    }
  }

  @Override
  public void transform(Function<Vec3d, Vec3d> transformation) {
    center = transformation.apply(center);
    updateRadius();
  }

  @Override
  public @NotNull RegionBuilderType getType() {
    return RegionBuilderTypes.SPHERE;
  }

  @Override
  public SphereRegionBuilder clone() {
    try {
      return (SphereRegionBuilder) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
