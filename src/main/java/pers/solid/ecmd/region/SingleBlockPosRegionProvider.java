package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.util.PositionProvider;

public record SingleBlockPosRegionProvider(EnhancedCoordinates pos) implements CuboidRegionProvider<SingleBlockPosRegion> {
  public static final MapCodec<SingleBlockPosRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(EnhancedCoordinates.CODEC.fieldOf("pos").forGetter(SingleBlockPosRegionProvider::pos)).apply(i, SingleBlockPosRegionProvider::new));

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
