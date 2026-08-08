package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import pers.solid.ecmd.util.ExecutionContext;

public record CuboidOutlineRegionProvider(BlockCuboidRegionProvider region, int thickness) implements RegionProvider<CuboidOutlineRegion> {
  public static final MapCodec<CuboidOutlineRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockCuboidRegionProvider.CODEC.fieldOf("region").forGetter(CuboidOutlineRegionProvider::region), Codec.INT.optionalFieldOf("thickness", 1).forGetter(CuboidOutlineRegionProvider::thickness)).apply(i, CuboidOutlineRegionProvider::new));

  @Override
  public CuboidOutlineRegion toAbsoluteRegion(ExecutionContext context) {
    return new CuboidOutlineRegion(region.toAbsoluteRegion(context), thickness);
  }

  @Override
  public RegionType<? super CuboidOutlineRegion> getType() {
    return RegionTypes.CUBOID_OUTLINE;
  }

  @Override
  public String expressAsString() {
    return "cuboid_outline(" + region.from().expressAsString() + ", " + region.to().expressAsString() + ", " + thickness + ")";
  }
}
