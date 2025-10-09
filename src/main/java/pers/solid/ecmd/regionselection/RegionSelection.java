package pers.solid.ecmd.regionselection;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.GeoUtil;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 区域选择可用于玩家在游戏内通过交互式的操作来建立一个区域。这个区域会在服务器和客户端之间进行同步，因此需要实现与 NBRegionBuilder 之间的转换。
 */
public interface RegionSelection {
  SimpleCommandExceptionType NOT_COMPLETED = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.region_selection.not_completed"));
  PacketCodec<RegistryByteBuf, RegionSelection> PACKET_CODEC = RegionSelectionType.PACKET_CODEC.dispatch(RegionSelection::getSelectionType, CacheBuilder.newBuilder().build(CacheLoader.from((@NotNull RegionSelectionType type) -> PacketCodec.of(RegionSelection::writePacket, (PacketByteBuf buf) -> {
    final RegionSelection regionSelection = type.createRegionSelection();
    regionSelection.readPacket(buf);
    return regionSelection;
  })))::getUnchecked);

  /**
   * 设置第一个点时的操作。有可能是直接设置的特定的点，也有可能是重新开始一个全新的区域。
   */
  Supplier<Text> clickFirstPoint(BlockPos point, PlayerEntity player);

  /**
   * 设置第二个点时的操作。有可能是直接设置的特定的点，也有可能是在多个点的列表中增加一个点。
   */
  Supplier<Text> clickSecondPoint(BlockPos point, PlayerEntity player);

  List<@NotNull Vec3d> getPoints();

  void readPoints(List<Vec3d> points);

  default void inheritPointsFrom(RegionSelection source) {
    readPoints(source.getPoints());
  }

  /**
   * 撤销上一个点的操作，仅限于有多个点时的操作。
   */
  default void popLastOperation(PlayerEntity player) {
    throw new UnsupportedOperationException();
  }

  /**
   * 移动选区自身，并通常返回自身。
   */
  @Contract(mutates = "this")
  default @NotNull RegionSelection moved(@NotNull Vec3i relativePos) {
    return moved(Vec3d.of(relativePos));
  }

  default @NotNull RegionSelection moved(@NotNull Vec3d relativePos) {
    return transformed(vec3d -> vec3d.add(relativePos));
  }

  default @NotNull RegionSelection rotated(@NotNull BlockRotation blockRotation, @NotNull Vec3d pivot) {
    return transformed(vec3d -> GeoUtil.rotate(vec3d, blockRotation, pivot));
  }

  default @NotNull RegionSelection mirrored(@NotNull Direction.Axis axis, @NotNull Vec3d pivot) {
    return transformed(vec3d -> GeoUtil.mirror(vec3d, axis, pivot));
  }

  default @NotNull RegionSelection expanded(double offset) {
    throw new UnsupportedOperationException();
  }

  /**
   * 区域沿指定坐标轴延伸浮点数值后的区域。
   */
  default @NotNull RegionSelection expanded(double offset, Direction.Axis axis) {
    throw new UnsupportedOperationException();
  }

  /**
   * 区域往指定方向延伸浮点数值后的区域，被延伸的那一侧是沿该方向最远的一侧。
   */
  default @NotNull RegionSelection expanded(double offset, Direction direction) {
    throw new UnsupportedOperationException();
  }

  default @NotNull RegionSelection expanded(double offset, Direction.Type type) {
    throw new UnsupportedOperationException();
  }

  /**
   * 对区域对象自身进行修改并返回自身。
   */
  @NotNull RegionSelection transformed(Function<Vec3d, Vec3d> transformation);

  /**
   * 转换为具体的 Region 对象。一般来说，它应该是缓存在对象的字段中的，如果自身有修改，则该字段清除，下次调用时再重新计算。
   */
  Region region();

  RegionSelection clone();

  @NotNull
  default String asString() {
    return region().asString();
  }

  @NotNull
  RegionSelectionType getSelectionType();

  @Contract(mutates = "this")
  void readNbt(@NotNull NbtCompound nbtCompound, @NotNull World world);

  void writeNbt(@NotNull NbtCompound nbtCompound);

  void readPacket(PacketByteBuf buf);

  void writePacket(PacketByteBuf buf);

  /**
   * <p>使用简单的线框来绘制这个区域，通常需要勾勒出该区域的大致图形以及关键点。
   * <p>注意：覆盖此方法时，需要标上 {@code @Environment(EnvType.CLIENT)}。
   *
   * @implNote 在给 vertexConsumer 提供的顶点坐标中，传入的坐标应该是相对坐标（具体的世界具坐标减去相机坐标），然后再转为浮点数传入 vertexConsumer 中，因为相对坐标通常是比较小的值，转化为浮点数也不损失精度。
   */
  @Environment(EnvType.CLIENT)
  void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d cameraPos);

}
