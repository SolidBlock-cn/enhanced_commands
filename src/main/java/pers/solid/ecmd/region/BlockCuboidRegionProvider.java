package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.util.PositionProvider;

public record BlockCuboidRegionProvider(EnhancedCoordinates from, EnhancedCoordinates to) implements CuboidRegionProvider<BlockCuboidRegion> {
  public static final MapCodec<BlockCuboidRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnhancedCoordinates.CODEC.fieldOf("from").forGetter(BlockCuboidRegionProvider::from),
      EnhancedCoordinates.CODEC.fieldOf("to").forGetter(BlockCuboidRegionProvider::to)
  ).apply(i, BlockCuboidRegionProvider::new));

  @Override
  public BlockCuboidRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new BlockCuboidRegion(from.toAbsoluteBlockPos(positionProvider), to.toAbsoluteBlockPos(positionProvider));
  }

  @Override
  public RegionType<BlockCuboidRegion> getType() {
    return RegionTypes.CUBOID;
  }

  @Override
  public String expressAsString() {
    return "cuboid(" + from.expressAsString() + ", " + to.expressAsString() + ")";
  }
}
