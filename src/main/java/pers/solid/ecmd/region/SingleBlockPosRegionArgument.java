package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.util.PositionProvider;

public record SingleBlockPosRegionArgument(EnhancedPosArgument pos) implements CuboidRegionArgument<SingleBlockPosRegion> {
  public static final MapCodec<SingleBlockPosRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(EnhancedPosArgument.CODEC.fieldOf("pos").forGetter(SingleBlockPosRegionArgument::pos)).apply(i, SingleBlockPosRegionArgument::new));

  @Override
  public SingleBlockPosRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new SingleBlockPosRegion(pos.toAbsoluteBlockPos(positionProvider));
  }

  @Override
  public @NotNull RegionType<SingleBlockPosRegion> getType() {
    return RegionTypes.SINGLE;
  }

  @Override
  public @NotNull String asString() {
    return "single(" + pos.asString() + ")";
  }
}
