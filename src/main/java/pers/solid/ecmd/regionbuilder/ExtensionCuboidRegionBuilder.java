package pers.solid.ecmd.regionbuilder;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.region.BlockCuboidRegion;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public class ExtensionCuboidRegionBuilder implements IntBackedRegionBuilder, Cloneable {
  public Vec3i firstPos;
  public Vec3i secondPos;

  @Override
  public void clickFirstPoint(BlockPos point, PlayerEntity player) {
    firstPos = point;
    secondPos = null;
    player.sendMessage(Text.translatable("enhancedCommands.argument.region_builder.cuboid.set_first", TextUtil.wrapBlockPos(firstPos).styled(TextUtil.STYLE_FOR_RESULT)));
    BlockCuboidRegionBuilder.notifyStatistics(firstPos, secondPos, player);
  }

  @Override
  public void clickSecondPoint(BlockPos point, PlayerEntity player) {
    if (firstPos == null) {
      firstPos = point;
      player.sendMessage(Text.translatable("enhancedCommands.argument.region_builder.cuboid.set_first", TextUtil.wrapBlockPos(firstPos).styled(TextUtil.STYLE_FOR_RESULT)));
    } else if (secondPos == null) {
      secondPos = point;
      player.sendMessage(Text.translatable("enhancedCommands.argument.region_builder.extension.include", TextUtil.wrapBlockPos(point).styled(TextUtil.STYLE_FOR_RESULT)));
    } else {
      final BlockBox blockBox = BlockBox.create(firstPos, secondPos);
      if (blockBox.contains(point)) {
        player.sendMessage(Text.translatable("enhancedCommands.argument.region_builder.extension.not_infected", TextUtil.wrapBlockPos(point).styled(TextUtil.STYLE_FOR_RESULT)));
        BlockCuboidRegionBuilder.notifyStatistics(firstPos, secondPos, player);
        return;
      }
      final BlockPos.Mutable mutable1 = new BlockPos.Mutable().set(firstPos);
      final BlockPos.Mutable mutable2 = new BlockPos.Mutable().set(secondPos);
      if (point.getX() > blockBox.getMaxX()) {
        (firstPos.getX() > secondPos.getX() ? mutable1 : mutable2).setX(point.getX());
      }
      if (point.getX() < blockBox.getMinX()) {
        (firstPos.getX() < secondPos.getX() ? mutable1 : mutable2).setX(point.getX());
      }
      if (point.getY() > blockBox.getMaxY()) {
        (firstPos.getY() > secondPos.getY() ? mutable1 : mutable2).setY(point.getY());
      }
      if (point.getY() < blockBox.getMinY()) {
        (firstPos.getY() < secondPos.getY() ? mutable1 : mutable2).setY(point.getY());
      }
      if (point.getZ() > blockBox.getMaxZ()) {
        (firstPos.getZ() > secondPos.getZ() ? mutable1 : mutable2).setZ(point.getZ());
      }
      if (point.getZ() < blockBox.getMinZ()) {
        (firstPos.getZ() < secondPos.getZ() ? mutable1 : mutable2).setZ(point.getZ());
      }
      if (!mutable1.equals(firstPos)) {
        firstPos = mutable1.toImmutable();
      }
      if (!mutable2.equals(secondPos)) {
        secondPos = mutable2.toImmutable();
      }
      player.sendMessage(Text.translatable("enhancedCommands.argument.region_builder.extension.include", TextUtil.wrapBlockPos(point).styled(TextUtil.STYLE_FOR_RESULT)));
      BlockCuboidRegionBuilder.notifyStatistics(firstPos, secondPos, player);
    }
  }

  @Override
  public List<@NotNull Vec3d> getPoints() {
    return Stream.of(firstPos, secondPos).filter(Objects::nonNull).map(Vec3d::ofCenter).toList();
  }

  @Override
  public void readPoints(List<Vec3d> points) {
    if (!points.isEmpty()) {
      firstPos = BlockPos.ofFloored(points.get(0));
      if (points.size() > 1) {
        secondPos = BlockPos.ofFloored(points.get(points.size() - 1));
      }
    }
  }

  @Override
  public Region buildRegion() throws CommandSyntaxException {
    if (firstPos == null || secondPos == null) {
      throw NOT_COMPLETED.create();
    } else {
      return new BlockCuboidRegion(firstPos, secondPos);
    }
  }

  @Override
  public @NotNull RegionBuilderType getType() {
    return RegionBuilderTypes.EXTENSION;
  }

  @Override
  public void transformInt(Function<Vec3i, Vec3i> transformation) {
    firstPos = transformation.apply(firstPos);
    secondPos = transformation.apply(secondPos);
  }

  @Override
  public ExtensionCuboidRegionBuilder clone() {
    try {
      return (ExtensionCuboidRegionBuilder) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
