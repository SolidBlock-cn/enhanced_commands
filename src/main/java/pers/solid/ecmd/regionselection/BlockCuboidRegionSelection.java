package pers.solid.ecmd.regionselection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.region.BlockCuboidRegion;
import pers.solid.ecmd.render.RegionRendering;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class BlockCuboidRegionSelection extends AbstractRegionSelection<BlockCuboidRegion> implements IntBackedRegionSelection, Cloneable {
  public static final MapCodec<BlockCuboidRegionSelection> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Vec3i.CODEC.optionalFieldOf("first").forGetter(s -> Optional.ofNullable(s.first)),
      Vec3i.CODEC.optionalFieldOf("second").forGetter(s -> Optional.ofNullable(s.second))
  ).apply(i, BlockCuboidRegionSelection::fromOptional));
  public @Nullable Vec3i first;
  public @Nullable Vec3i second;

  private static BlockCuboidRegionSelection fromOptional(Optional<Vec3i> first, Optional<Vec3i> second) {
    final BlockCuboidRegionSelection n = new BlockCuboidRegionSelection();
    n.first = first.orElse(null);
    n.second = second.orElse(null);
    return n;
  }

  public static Component notifyStatistics(@Nullable Vec3i first, @Nullable Vec3i second) {
    if (first != null && second != null) {
      final Vec3i subtract = first.subtract(second);
      final int dx = Math.abs(subtract.getX()) + 1;
      final int dy = Math.abs(subtract.getY()) + 1;
      final int dz = Math.abs(subtract.getZ()) + 1;
      return (Component.translatable("enhanced_commands.region_selection.cuboid.statistics", Component.literal(dx + "×" + dy + "×" + dz).withStyle(Styles.RESULT), TextUtil.literal((long) dx * dy * dz).withStyle(Styles.RESULT)).withStyle(ChatFormatting.GRAY));
    } else {
      return null;
    }
  }

  @Override
  public Supplier<Component> clickFirstPoint(Vec3 point, Player player) {
    first = BlockPos.containing(point);
    resetCalculation();
    return () -> TextUtil.joinNullableLines(Component.translatable("enhanced_commands.region_selection.cuboid.set_first", TextUtil.wrapVector(first).withStyle(Styles.RESULT)), notifyStatistics(first, second));
  }

  @Override
  public Supplier<Component> clickSecondPoint(Vec3 point, Player player) {
    second = BlockPos.containing(point);
    resetCalculation();
    return () -> TextUtil.joinNullableLines(Component.translatable("enhanced_commands.region_selection.cuboid.set_second", TextUtil.wrapVector(second).withStyle(Styles.RESULT)), notifyStatistics(first, second));
  }

  @Override
  public List<Vec3> getPoints() {
    return Stream.of(first, second).filter(Objects::nonNull).map(Vec3::atCenterOf).toList();
  }

  @Override
  public void readPoints(List<Vec3> points) {
    if (!points.isEmpty()) {
      first = BlockPos.containing(points.get(0));
    }
    if (points.size() > 1) {
      second = BlockPos.containing(points.get(points.size() - 1));
    }
    resetCalculation();
  }

  @Override
  public BlockCuboidRegion buildRegion() throws CommandSyntaxException {
    if (first == null || second == null) {
      throw NOT_COMPLETED.create();
    } else {
      return new BlockCuboidRegion(first, second);
    }
  }

  @Override
  public RegionSelection expanded(int offset) throws CommandSyntaxException {
    if (first == null || second == null) {
      throw NOT_COMPLETED.create();
    }
    int x1 = first.getX();
    int y1 = first.getY();
    int z1 = first.getZ();
    int x2 = second.getX();
    int y2 = second.getY();
    int z2 = second.getZ();
    final Vec3i pos1Offset = new Vec3i(x1 > x2 ? offset : -offset, y1 > y2 ? offset : -offset, z1 > z2 ? offset : -offset);
    first = first.offset(pos1Offset);
    second = second.subtract(pos1Offset);
    resetCalculation();
    return this;
  }

  @Override
  public RegionSelection expanded(int offset, Direction direction) throws CommandSyntaxException {
    if (first == null || second == null) {
      throw NOT_COMPLETED.create();
    }
    final Vec3i vector = direction.getNormal();
    final Direction.Axis axis = direction.getAxis();
    int unitPosOffset = vector.get(axis);
    int coord1 = first.get(axis);
    int coord2 = second.get(axis);
    if ((coord1 > coord2) == (unitPosOffset > 0)) {
      // unitPosOffset > 0，且 coord1 较大，或者两个都相反，
      // 此时修改 first
      first = first.offset(vector.multiply(offset));
    } else {
      // 此时修改 second
      second = second.offset(vector.multiply(offset));
    }
    resetCalculation();
    return this;
  }

  @Override
  public RegionSelection expanded(int offset, Direction.Axis axis) throws CommandSyntaxException {
    if (first == null || second == null) {
      throw NOT_COMPLETED.create();
    }
    final Vec3i pos1Offset = new Vec3i(first.getX() > second.getX() ? offset : -offset, first.getY() > second.getY() ? offset : -offset, first.getZ() > second.getZ() ? offset : -offset);

    first = first.offset(pos1Offset);
    second = second.subtract(pos1Offset);
    resetCalculation();
    return this;
  }

  @Override
  public RegionSelection expanded(int offset, Direction.Plane type) throws CommandSyntaxException {
    if (first == null || second == null) {
      throw NOT_COMPLETED.create();
    }
    final Vec3i pos1Offset = switch (type) {
      case VERTICAL -> new Vec3i(0, first.getY() > second.getY() ? offset : -offset, 0);
      case HORIZONTAL -> new Vec3i(first.getX() > second.getX() ? offset : -offset, 0, first.getZ() > second.getZ() ? offset : -offset);
    };
    first = first.offset(pos1Offset);
    second = second.subtract(pos1Offset);
    resetCalculation();
    return this;
  }

  @Override
  public RegionSelectionType getType() {
    return RegionSelectionTypes.CUBOID;
  }

  @Override
  public IntBackedRegionSelection transformedInt(Function<Vec3i, Vec3i> transformation) {
    first = first == null ? null : transformation.apply(first);
    second = second == null ? null : transformation.apply(second);
    resetCalculation();
    return this;
  }

  @Override
  public IntBackedRegionSelection clone() {
    return (IntBackedRegionSelection) super.clone();
  }

  @Override
  public void readPacket(FriendlyByteBuf buf) {
    final boolean firstPresent = buf.readBoolean();
    first = firstPresent ? new Vec3i(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()) : null;
    final boolean secondPresent = buf.readBoolean();
    second = secondPresent ? new Vec3i(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()) : null;
  }

  @Override
  public void serializeToNetwork(FriendlyByteBuf buf) {
    buf.writeBoolean(first != null);
    if (first != null) {
      buf.writeVarInt(first.getX());
      buf.writeVarInt(first.getY());
      buf.writeVarInt(first.getZ());
    }
    buf.writeBoolean(second != null);
    if (second != null) {
      buf.writeVarInt(second.getX());
      buf.writeVarInt(second.getY());
      buf.writeVarInt(second.getZ());
    }
  }

  @Environment(EnvType.CLIENT)
  @Override
  public void render(PoseStack matrices, MultiBufferSource vertexConsumers, Vec3 cameraPos) {
    RegionRendering.renderBlockCuboid(first, second, matrices, vertexConsumers.getBuffer(RegionRendering.regionRenderLayer), cameraPos);
  }

}
