package pers.solid.ecmd.util.enums;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public enum OutlineType implements StringRepresentable {

  /**
   * The pos itself is in the region, but one if its near pos is not in the region.
   */
  OUTLINE("outline") {
    @Override
    public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
      return Direction.stream().map(blockPos::relative);
    }
  },
  OUTLINE_CONNECTED("outline_connected") {
    @Override
    public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
      return BlockPos.betweenClosedStream(-1, -1, -1, 1, 1, 1).filter(blockPos1 -> blockPos1 != BlockPos.ZERO).map(blockPos::offset);
    }
  },
  WALL("wall") {
    @Override
    public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
      return Direction.Plane.HORIZONTAL.stream().map(blockPos::relative);
    }
  },
  WALL_CONNECTED("wall_connected") {
    @Override
    public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
      return BlockPos.betweenClosedStream(-1, 0, -1, 1, 0, 1).filter(blockPos1 -> blockPos1 != BlockPos.ZERO).map(blockPos::offset);
    }
  },
  FLOOR_AND_CEIL("floor_and_ceil") {
    @Override
    public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
      return Stream.of(blockPos.above(), blockPos.below());
    }
  };

  public static final StringIdentifiableCodec<OutlineType> CODEC = StringIdentifiableCodec.create(OutlineType.values());
  public static final MapCodec<OutlineType> OUTLINE_TYPE_FIELD = OutlineType.CODEC.optionalFieldOf("outline_type").xmap(o -> o.orElse(OutlineType.OUTLINE), Optional::of);
  private final String name;

  OutlineType(String name) {
    this.name = name;
  }

  public abstract Stream<BlockPos> streamNearbyPos(BlockPos blockPos);

  public boolean modifiedTest(Predicate<BlockPos> predicate, BlockPos blockPos) {
    return predicate.test(blockPos) && streamNearbyPos(blockPos).anyMatch(modifiedPos -> !predicate.test(modifiedPos));
  }

  @Override
  public @NotNull String getSerializedName() {
    return name;
  }

  public MutableComponent getDisplayName() {
    return Component.translatable("enhanced_commands.outline_type." + name);
  }
}
