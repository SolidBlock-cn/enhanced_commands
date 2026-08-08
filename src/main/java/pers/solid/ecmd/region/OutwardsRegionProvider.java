package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.util.ExecutionContext;

public record OutwardsRegionProvider(EnhancedCoordinates center, int x, int y, int z) implements RegionProvider<OutwardsRegion> {
  public static final MapCodec<OutwardsRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(EnhancedCoordinates.CODEC.fieldOf("center").forGetter(OutwardsRegionProvider::center), Codec.INT.fieldOf("x").forGetter(OutwardsRegionProvider::x), Codec.INT.fieldOf("y").forGetter(OutwardsRegionProvider::y), Codec.INT.fieldOf("z").forGetter(OutwardsRegionProvider::z)).apply(i, OutwardsRegionProvider::new));

  @Override
  public OutwardsRegion toAbsoluteRegion(ExecutionContext context) {
    return new OutwardsRegion(center.toAbsoluteBlockPos(context.positionProvider), x, y, z);
  }

  @Override
  public RegionType<OutwardsRegion> getType() {
    return RegionTypes.OUTWARDS;
  }

  @Override
  public String expressAsString() {
    return "outwards(" + center.expressAsString() + ", " + x + " " + y + " " + z + ")";
  }
}
