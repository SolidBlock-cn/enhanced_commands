package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.util.PositionProvider;

public record SphereRegionArgument(double radius, EnhancedPosArgument center) implements RegionArgument<SphereRegion> {
  public static final MapCodec<SphereRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.DOUBLE.fieldOf("radius").forGetter(SphereRegionArgument::radius),
      EnhancedPosArgument.CODEC.fieldOf("center").forGetter(SphereRegionArgument::center)
  ).apply(i, SphereRegionArgument::new));

  @Override
  public SphereRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new SphereRegion(radius, center.toAbsolutePos(positionProvider));
  }

  @Override
  public @NotNull RegionType<SphereRegion> getType() {
    return RegionTypes.SPHERE;
  }

  @Override
  public @NotNull String asString() {
    return "sphere(" + radius + ", " + center.asString() + ")";
  }
}
