package pers.solid.ecmd.regionbuilder;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
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
import java.util.stream.Stream;

public class BlockCuboidRegionBuilder implements IntBackedRegionBuilder, Cloneable {
  public Vec3i first;
  public Vec3i second;

  @Override
  public void clickFirstPoint(BlockPos point, PlayerEntity player) {
    first = point;
    player.sendMessage(Text.translatable("enhancedCommands.argument.region_builder.cuboid.set_first", TextUtil.wrapBlockPos(first).styled(TextUtil.STYLE_FOR_RESULT)));
    notifyStatistics(first, second, player);
  }

  @Override
  public void clickSecondPoint(BlockPos point, PlayerEntity player) {
    second = point;
    player.sendMessage(Text.translatable("enhancedCommands.argument.region_builder.cuboid.set_second", TextUtil.wrapBlockPos(second).styled(TextUtil.STYLE_FOR_RESULT)));
    notifyStatistics(first, second, player);
  }

  public static void notifyStatistics(@Nullable Vec3i first, @Nullable Vec3i second, PlayerEntity player) {
    if (first != null && second != null) {
      final Vec3i subtract = first.subtract(second);
      final int dx = Math.abs(subtract.getX()) + 1;
      final int dy = Math.abs(subtract.getY()) + 1;
      final int dz = Math.abs(subtract.getZ()) + 1;
      player.sendMessage(Text.translatable("enhancedCommands.argument.region_builder.cuboid.statistics", Text.literal(dx + "×" + dy + "×" + dz).styled(TextUtil.STYLE_FOR_RESULT), TextUtil.literal(dx * dy * dz).styled(TextUtil.STYLE_FOR_RESULT)).formatted(Formatting.GRAY));
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
  }

  @Override
  public Region buildRegion() throws CommandSyntaxException {
    if (first == null || second == null) {
      throw NOT_COMPLETED.create();
    } else {
      return new BlockCuboidRegion(first, second);
    }
  }

  @Override
  public @NotNull RegionBuilderType getType() {
    return RegionBuilderTypes.CUBOID;
  }

  @Override
  public void transformInt(Function<Vec3i, Vec3i> transformation) {
    first = transformation.apply(first);
    second = transformation.apply(second);
  }

  @Override
  public IntBackedRegionBuilder clone() {
    try {
      return (IntBackedRegionBuilder) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
