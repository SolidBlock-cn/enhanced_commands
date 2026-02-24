package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.enums.OutlineType;

public record OutlineRegionProvider(OutlineType outlineType, RegionProvider<?> region) implements RegionProvider<OutlineRegion> {
  public static final MapCodec<OutlineRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(OutlineType.OUTLINE_TYPE_FIELD.forGetter(OutlineRegionProvider::outlineType), RegionProvider.CODEC.fieldOf("region").forGetter(OutlineRegionProvider::region)).apply(i, OutlineRegionProvider::new));

  @Override
  public OutlineRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new OutlineRegion(outlineType, region.toAbsoluteRegion(positionProvider));
  }

  @Override
  public @NotNull RegionType<? super OutlineRegion> getType() {
    return RegionTypes.OUTLINE;
  }

  @Override
  public @NotNull String asString() {
    return "outline(" + region.asString() + ", " + outlineType.getSerializedName() + ")";
  }
}
