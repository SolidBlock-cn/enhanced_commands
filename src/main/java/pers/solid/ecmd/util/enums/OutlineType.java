package pers.solid.ecmd.util.enums;

import com.mojang.serialization.MapCodec;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public enum OutlineType implements StringIdentifiable {

  /**
   * The pos itself is in the region, but one if its near pos is not in the region.
   */
  OUTLINE("outline") {
    @Override
    public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
      return Direction.stream().map(blockPos::offset);
    }
  },
  OUTLINE_CONNECTED("outline_connected") {
    @Override
    public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
      return BlockPos.stream(-1, -1, -1, 1, 1, 1).filter(blockPos1 -> blockPos1 != BlockPos.ORIGIN).map(blockPos::add);
    }
  },
  WALL("wall") {
    @Override
    public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
      return Direction.Type.HORIZONTAL.stream().map(blockPos::offset);
    }
  },
  WALL_CONNECTED("wall_connected") {
    @Override
    public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
      return BlockPos.stream(-1, 0, -1, 1, 0, 1).filter(blockPos1 -> blockPos1 != BlockPos.ORIGIN).map(blockPos::add);
    }
  },
  FLOOR_AND_CEIL("floor_and_ceil") {
    @Override
    public Stream<BlockPos> streamNearbyPos(BlockPos blockPos) {
      return Stream.of(blockPos.up(), blockPos.down());
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
  public String asString() {
    return name;
  }

  public MutableText getDisplayName() {
    return Text.translatable("enhanced_commands.outline_type." + name);
  }
}
