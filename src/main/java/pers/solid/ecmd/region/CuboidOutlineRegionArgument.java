package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.PositionProvider;

public record CuboidOutlineRegionArgument(BlockCuboidRegionArgument region, int thickness) implements RegionArgument<CuboidOutlineRegion> {
  public static final MapCodec<CuboidOutlineRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockCuboidRegionArgument.CODEC.fieldOf("region").forGetter(CuboidOutlineRegionArgument::region), Codec.INT.optionalFieldOf("thickness", 1).forGetter(CuboidOutlineRegionArgument::thickness)).apply(i, CuboidOutlineRegionArgument::new));

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
