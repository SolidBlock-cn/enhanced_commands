package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.util.PositionProvider;

public record OutwardsRegionProvider(EnhancedCoordinates center, int x, int y, int z) implements RegionProvider<OutwardsRegion> {
  public static final MapCodec<OutwardsRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(EnhancedCoordinates.CODEC.fieldOf("center").forGetter(OutwardsRegionProvider::center), Codec.INT.fieldOf("x").forGetter(OutwardsRegionProvider::x), Codec.INT.fieldOf("y").forGetter(OutwardsRegionProvider::y), Codec.INT.fieldOf("z").forGetter(OutwardsRegionProvider::z)).apply(i, OutwardsRegionProvider::new));

  @Override
  public OutwardsRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new OutwardsRegion(center.toAbsoluteBlockPos(positionProvider), x, y, z);
  }

  @Override
  public @NotNull RegionType<OutwardsRegion> getType() {
    return RegionTypes.OUTWARDS;
  }

  @Override
  public @NotNull String asString() {
    return "outwards(" + center.asString() + ", " + x + " " + y + " " + z + ")";
  }
}
