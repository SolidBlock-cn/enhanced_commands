package pers.solid.ecmd.regionselection;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
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
  public Supplier<Component> clickFirstPoint(Vec3 point, Player player) {
    first = BlockPos.containing(point);
    second = null;
    resetCalculation();
    return () -> TextUtil.joinNullableLines(Component.translatable("enhanced_commands.region_selection.cuboid.set_first", TextUtil.wrapVector(first).withStyle(Styles.RESULT)), BlockCuboidRegionSelection.notifyStatistics(first, second));
  }

  @Override
  public Supplier<Component> clickSecondPoint(Vec3 point, Player player) {
    final BlockPos pos = BlockPos.containing(point);
    if (first == null) {
      first = pos;
      return () -> (Component.translatable("enhanced_commands.region_selection.cuboid.set_first", TextUtil.wrapVector(first).withStyle(Styles.RESULT)));
    } else if (second == null) {
      second = pos;
      resetCalculation();
      return () -> (Component.translatable("enhanced_commands.region_selection.extension.include", TextUtil.wrapVector(pos).withStyle(Styles.RESULT)));
    } else {
      final BoundingBox blockBox = BoundingBox.fromCorners(first, second);
      if (blockBox.isInside(pos)) {
        return () -> TextUtil.joinNullableLines(Component.translatable("enhanced_commands.region_selection.extension.not_infected", TextUtil.wrapVector(pos).withStyle(Styles.RESULT)), BlockCuboidRegionSelection.notifyStatistics(first, second));
      }
      final BlockPos.MutableBlockPos mutable1 = new BlockPos.MutableBlockPos().set(first);
      final BlockPos.MutableBlockPos mutable2 = new BlockPos.MutableBlockPos().set(second);
      if (pos.getX() > blockBox.maxX()) {
        (first.getX() > second.getX() ? mutable1 : mutable2).setX(pos.getX());
      }
      if (pos.getX() < blockBox.minX()) {
        (first.getX() < second.getX() ? mutable1 : mutable2).setX(pos.getX());
      }
      if (pos.getY() > blockBox.maxY()) {
        (first.getY() > second.getY() ? mutable1 : mutable2).setY(pos.getY());
      }
      if (pos.getY() < blockBox.minY()) {
        (first.getY() < second.getY() ? mutable1 : mutable2).setY(pos.getY());
      }
      if (pos.getZ() > blockBox.maxZ()) {
        (first.getZ() > second.getZ() ? mutable1 : mutable2).setZ(pos.getZ());
      }
      if (pos.getZ() < blockBox.minZ()) {
        (first.getZ() < second.getZ() ? mutable1 : mutable2).setZ(pos.getZ());
      }
      if (!mutable1.equals(first)) {
        first = mutable1.immutable();
      }
      if (!mutable2.equals(second)) {
        second = mutable2.immutable();
      }
      resetCalculation();
      return () -> TextUtil.joinNullableLines(Component.translatable("enhanced_commands.region_selection.extension.include", TextUtil.wrapVector(pos).withStyle(Styles.RESULT)), BlockCuboidRegionSelection.notifyStatistics(first, second));
    }
  }

  @Override
  public void readPoints(List<Vec3> points) {
    if (!points.isEmpty()) {
      first = BlockPos.containing(points.get(0));
      if (points.size() > 1) {
        second = BlockPos.containing(points.get(points.size() - 1));
      }
      resetCalculation();
    }
  }

  @Override
  public RegionSelectionType getType() {
    return RegionSelectionTypes.EXTENSION;
  }


  @Override
  public ExtensionCuboidRegionSelection clone() {
    return (ExtensionCuboidRegionSelection) super.clone();
  }
}
