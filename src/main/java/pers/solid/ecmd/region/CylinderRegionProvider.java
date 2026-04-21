package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Range;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.StringUtil;

public record CylinderRegionProvider(@Range(from = 0, to = Long.MAX_VALUE) double radius, @Range(from = 0, to = Long.MAX_VALUE) double height, EnhancedCoordinates center) implements RegionProvider<CylinderRegion> {
  public static final MapCodec<CylinderRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codec.DOUBLE.fieldOf("radius").forGetter(CylinderRegionProvider::radius), Codec.DOUBLE.fieldOf("height").forGetter(CylinderRegionProvider::height), EnhancedCoordinates.CODEC.fieldOf("center").forGetter(CylinderRegionProvider::center)).apply(i, CylinderRegionProvider::new));

  @Override
  public CylinderRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new CylinderRegion(radius, height, center.toAbsolutePos(positionProvider));
  }

  @Override
  public RegionType<CylinderRegion> getType() {
    return RegionTypes.CYLINDER;
  }

  @Override
  public String expressAsString() {
    return "cylinder(" + StringUtil.nf.format(radius) + ", " + StringUtil.nf.format(height) + ", " + center.expressAsString() + ")";
  }
}
