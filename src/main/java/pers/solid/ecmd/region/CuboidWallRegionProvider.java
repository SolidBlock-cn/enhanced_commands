package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import pers.solid.ecmd.util.ExecutionContext;

public record CuboidWallRegionProvider(BlockCuboidRegionProvider region, int thickness) implements RegionProvider<CuboidWallRegion> {
  public static final MapCodec<CuboidWallRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockCuboidRegionProvider.CODEC.fieldOf("region").forGetter(CuboidWallRegionProvider::region), Codec.INT.optionalFieldOf("thickness", 1).forGetter(CuboidWallRegionProvider::thickness)).apply(i, CuboidWallRegionProvider::new));

  @Override
  public CuboidWallRegion toAbsoluteRegion(ExecutionContext context) {
    return new CuboidWallRegion(region.toAbsoluteRegion(context), thickness);
  }

  @Override
  public RegionType<? super CuboidWallRegion> getType() {
    return RegionTypes.CUBOID_WALL;
  }

  @Override
  public String expressAsString() {
    return "cuboid_wall(" + region.from().expressAsString() + ", " + region.to().expressAsString() + ", " + thickness + ")";
  }
}
