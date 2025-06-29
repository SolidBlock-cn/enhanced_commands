package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.util.PositionProvider;

public record OutwardsRegionArgument(EnhancedPosArgument center, int x, int y, int z) implements RegionArgument<OutwardsRegion> {
  public static final MapCodec<OutwardsRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(EnhancedPosArgument.CODEC.fieldOf("center").forGetter(OutwardsRegionArgument::center), Codec.INT.fieldOf("x").forGetter(OutwardsRegionArgument::x), Codec.INT.fieldOf("y").forGetter(OutwardsRegionArgument::y), Codec.INT.fieldOf("z").forGetter(OutwardsRegionArgument::z)).apply(i, OutwardsRegionArgument::new));

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
