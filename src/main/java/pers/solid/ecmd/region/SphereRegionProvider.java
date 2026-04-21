package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.StringUtil;

public record SphereRegionProvider(double radius, EnhancedCoordinates center) implements RegionProvider<SphereRegion> {
  public static final MapCodec<SphereRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.DOUBLE.fieldOf("radius").forGetter(SphereRegionProvider::radius),
      EnhancedCoordinates.CODEC.fieldOf("center").forGetter(SphereRegionProvider::center)
  ).apply(i, SphereRegionProvider::new));

  @Override
  public SphereRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new SphereRegion(radius, center.toAbsolutePos(positionProvider));
  }

  @Override
  public RegionType<SphereRegion> getType() {
    return RegionTypes.SPHERE;
  }

  @Override
  public String expressAsString() {
    return "sphere(" + StringUtil.nf.format(radius) + ", " + center.expressAsString() + ")";
  }
}
