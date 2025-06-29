package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.util.PositionProvider;

public record PreciseCuboidRegionArgument(EnhancedPosArgument from, EnhancedPosArgument to) implements CuboidRegionArgument<PreciseCuboidRegion> {
  public static final MapCodec<PreciseCuboidRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnhancedPosArgument.CODEC.fieldOf("from").forGetter(PreciseCuboidRegionArgument::from),
      EnhancedPosArgument.CODEC.fieldOf("to").forGetter(PreciseCuboidRegionArgument::to)
  ).apply(i, PreciseCuboidRegionArgument::new));

  @Override
  public PreciseCuboidRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new PreciseCuboidRegion(from.toAbsolutePos(positionProvider), to.toAbsolutePos(positionProvider));
  }

  @Override
  public @NotNull RegionType<? super PreciseCuboidRegion> getType() {
    return RegionTypes.CUBOID;
  }

  @Override
  public @NotNull String asString() {
    return "cuboid(" + from.asString() + ", " + to.asString() + ")";
  }
}
