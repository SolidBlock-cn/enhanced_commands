package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.enums.OutlineType;

public record HollowCylinderRegionArgument(@NotNull OutlineType outlineType, @NotNull CylinderRegionArgument region) implements RegionArgument<HollowCylinderRegion> {
  public static final MapCodec<HollowCylinderRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
          OutlineType.OUTLINE_TYPE_FIELD.forGetter(HollowCylinderRegionArgument::outlineType),
          CylinderRegionArgument.CODEC.fieldOf("region").forGetter(HollowCylinderRegionArgument::region))
      .apply(i, HollowCylinderRegionArgument::new));

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
    return "hcyl(" + region.radius() + ", " + region.height() + ", " + region.center().asString() + ", " + outlineType.asString() + ")";
  }
}
