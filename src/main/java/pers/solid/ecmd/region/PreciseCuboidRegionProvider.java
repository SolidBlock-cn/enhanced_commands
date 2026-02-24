package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.util.PositionProvider;

public record PreciseCuboidRegionProvider(EnhancedCoordinates from, EnhancedCoordinates to) implements CuboidRegionProvider<PreciseCuboidRegion> {
  public static final MapCodec<PreciseCuboidRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnhancedCoordinates.CODEC.fieldOf("from").forGetter(PreciseCuboidRegionProvider::from),
      EnhancedCoordinates.CODEC.fieldOf("to").forGetter(PreciseCuboidRegionProvider::to)
  ).apply(i, PreciseCuboidRegionProvider::new));

  @Override
  public PreciseCuboidRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new PreciseCuboidRegion(from.toAbsolutePos(positionProvider), to.toAbsolutePos(positionProvider));
  }

  @Override
  public @NotNull RegionType<? super PreciseCuboidRegion> getType() {
    return RegionTypes.CUBOID_PRECISE;
  }

  @Override
  public @NotNull String asString() {
    return "cuboid(" + from.asString() + ", " + to.asString() + ")";
  }
}
