package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.util.ExecutionContext;

public record SingleBlockPosRegionProvider(EnhancedCoordinates pos) implements CuboidRegionProvider<SingleBlockPosRegion> {
  public static final MapCodec<SingleBlockPosRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(EnhancedCoordinates.CODEC.fieldOf("pos").forGetter(SingleBlockPosRegionProvider::pos)).apply(i, SingleBlockPosRegionProvider::new));

  @Override
  public SingleBlockPosRegion toAbsoluteRegion(ExecutionContext context) {
    return new SingleBlockPosRegion(pos.toAbsoluteBlockPos(context.positionProvider));
  }

  @Override
  public RegionType<SingleBlockPosRegion> getType() {
    return RegionTypes.SINGLE;
  }

  @Override
  public String expressAsString() {
    return "single(" + pos.expressAsString() + ")";
  }
}
