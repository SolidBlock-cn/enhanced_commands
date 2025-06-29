package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.PositionProvider;

public record CuboidWallRegionArgument(BlockCuboidRegionArgument region, int thickness) implements RegionArgument<CuboidWallRegion> {
  public static final MapCodec<CuboidWallRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockCuboidRegionArgument.CODEC.fieldOf("region").forGetter(CuboidWallRegionArgument::region), Codec.INT.optionalFieldOf("thickness", 1).forGetter(CuboidWallRegionArgument::thickness)).apply(i, CuboidWallRegionArgument::new));

  @Override
  public CuboidWallRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new CuboidWallRegion(region.toAbsoluteRegion(positionProvider), thickness);
  }

  @Override
  public @NotNull RegionType<? super CuboidWallRegion> getType() {
    return RegionTypes.CUBOID_WALL;
  }

  @Override
  public @NotNull String asString() {
    return "cuboid_wall(" + region.from().asString() + ", " + region.to().asString() + ", " + thickness + ")";
  }
}
