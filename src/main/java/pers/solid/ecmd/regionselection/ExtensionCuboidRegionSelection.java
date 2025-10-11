package pers.solid.ecmd.regionselection;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ExtensionCuboidRegionSelection extends BlockCuboidRegionSelection {
  public static final MapCodec<ExtensionCuboidRegionSelection> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Vec3i.CODEC.optionalFieldOf("first").forGetter(s -> Optional.ofNullable(s.first)),
      Vec3i.CODEC.optionalFieldOf("second").forGetter(s -> Optional.ofNullable(s.second))
  ).apply(i, ExtensionCuboidRegionSelection::fromOptional));

  private static ExtensionCuboidRegionSelection fromOptional(Optional<Vec3i> first, Optional<Vec3i> second) {
    final ExtensionCuboidRegionSelection n = new ExtensionCuboidRegionSelection();
    n.first = first.orElse(null);
    n.second = second.orElse(null);
    return n;
  }

  @Override
  public Supplier<Text> clickFirstPoint(Vec3d point, PlayerEntity player) {
    first = BlockPos.ofFloored(point);
    second = null;
    resetCalculation();
    return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.region_selection.cuboid.set_first", TextUtil.wrapVector(first).styled(Styles.RESULT)), BlockCuboidRegionSelection.notifyStatistics(first, second));
  }

  @Override
  public Supplier<Text> clickSecondPoint(Vec3d point, PlayerEntity player) {
    final BlockPos pos = BlockPos.ofFloored(point);
    if (first == null) {
      first = pos;
      return () -> (Text.translatable("enhanced_commands.region_selection.cuboid.set_first", TextUtil.wrapVector(first).styled(Styles.RESULT)));
    } else if (second == null) {
      second = pos;
      resetCalculation();
      return () -> (Text.translatable("enhanced_commands.region_selection.extension.include", TextUtil.wrapVector(pos).styled(Styles.RESULT)));
    } else {
      final BlockBox blockBox = BlockBox.create(first, second);
      if (blockBox.contains(pos)) {
        return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.region_selection.extension.not_infected", TextUtil.wrapVector(pos).styled(Styles.RESULT)), BlockCuboidRegionSelection.notifyStatistics(first, second));
      }
      final BlockPos.Mutable mutable1 = new BlockPos.Mutable().set(first);
      final BlockPos.Mutable mutable2 = new BlockPos.Mutable().set(second);
      if (pos.getX() > blockBox.getMaxX()) {
        (first.getX() > second.getX() ? mutable1 : mutable2).setX(pos.getX());
      }
      if (pos.getX() < blockBox.getMinX()) {
        (first.getX() < second.getX() ? mutable1 : mutable2).setX(pos.getX());
      }
      if (pos.getY() > blockBox.getMaxY()) {
        (first.getY() > second.getY() ? mutable1 : mutable2).setY(pos.getY());
      }
      if (pos.getY() < blockBox.getMinY()) {
        (first.getY() < second.getY() ? mutable1 : mutable2).setY(pos.getY());
      }
      if (pos.getZ() > blockBox.getMaxZ()) {
        (first.getZ() > second.getZ() ? mutable1 : mutable2).setZ(pos.getZ());
      }
      if (pos.getZ() < blockBox.getMinZ()) {
        (first.getZ() < second.getZ() ? mutable1 : mutable2).setZ(pos.getZ());
      }
      if (!mutable1.equals(first)) {
        first = mutable1.toImmutable();
      }
      if (!mutable2.equals(second)) {
        second = mutable2.toImmutable();
      }
      resetCalculation();
      return () -> TextUtil.joinNullableLines(Text.translatable("enhanced_commands.region_selection.extension.include", TextUtil.wrapVector(pos).styled(Styles.RESULT)), BlockCuboidRegionSelection.notifyStatistics(first, second));
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
  public @NotNull RegionSelectionType getType() {
    return RegionSelectionTypes.EXTENSION;
  }


  @Override
  public @NotNull ExtensionCuboidRegionSelection clone() {
    return (ExtensionCuboidRegionSelection) super.clone();
  }
}
