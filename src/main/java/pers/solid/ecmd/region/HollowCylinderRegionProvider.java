package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.enums.OutlineType;

public record HollowCylinderRegionProvider(@NotNull OutlineType outlineType, @NotNull CylinderRegionProvider region) implements RegionProvider<HollowCylinderRegion> {
  public static final MapCodec<HollowCylinderRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
          OutlineType.OUTLINE_TYPE_FIELD.forGetter(HollowCylinderRegionProvider::outlineType),
          CylinderRegionProvider.CODEC.fieldOf("region").forGetter(HollowCylinderRegionProvider::region))
      .apply(i, HollowCylinderRegionProvider::new));

  @Override
  public HollowCylinderRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new HollowCylinderRegion(outlineType, region.toAbsoluteRegion(positionProvider));
  }

  @Override
  public @NotNull RegionType<HollowCylinderRegion> getType() {
    return RegionTypes.HOLLOW_CYLINDER;
  }

  @Override
  public @NotNull String asString() {
    return "hcyl(" + StringUtil.nf.format(region.radius()) + ", " + StringUtil.nf.format(region.height()) + ", " + region.center().asString() + ", " + outlineType.getSerializedName() + ")";
  }
}
