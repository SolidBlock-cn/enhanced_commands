package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.util.PositionProvider;

public record CylinderRegionArgument(@Range(from = 0, to = Long.MAX_VALUE) double radius, @Range(from = 0, to = Long.MAX_VALUE) double height, @NotNull EnhancedPosArgument center) implements RegionArgument<CylinderRegion> {
  public static final MapCodec<CylinderRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codec.DOUBLE.fieldOf("radius").forGetter(CylinderRegionArgument::radius), Codec.DOUBLE.fieldOf("height").forGetter(CylinderRegionArgument::height), EnhancedPosArgument.CODEC.fieldOf("center").forGetter(CylinderRegionArgument::center)).apply(i, CylinderRegionArgument::new));

  @Override
  public CylinderRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new CylinderRegion(radius, height, center.toAbsolutePos(positionProvider));
  }

  @Override
  public @NotNull RegionType<CylinderRegion> getType() {
    return RegionTypes.CYLINDER;
  }

  @Override
  public @NotNull String asString() {
    return "cylinder(" + radius + ", " + height + ", " + center.asString() + ")";
  }
}
