package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.PositionProvider;

public record CuboidOutlineRegionProvider(BlockCuboidRegionProvider region, int thickness) implements RegionProvider<CuboidOutlineRegion> {
  public static final MapCodec<CuboidOutlineRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockCuboidRegionProvider.CODEC.fieldOf("region").forGetter(CuboidOutlineRegionProvider::region), Codec.INT.optionalFieldOf("thickness", 1).forGetter(CuboidOutlineRegionProvider::thickness)).apply(i, CuboidOutlineRegionProvider::new));

  @Override
  public CuboidOutlineRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new CuboidOutlineRegion(region.toAbsoluteRegion(positionProvider), thickness);
  }

  @Override
  public @NotNull RegionType<? super CuboidOutlineRegion> getType() {
    return RegionTypes.CUBOID_OUTLINE;
  }

  @Override
  public @NotNull String asString() {
    return "cuboid_outline(" + region.from().asString() + ", " + region.to().asString() + ", " + thickness + ")";
  }
}
