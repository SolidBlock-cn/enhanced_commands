package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.enums.OutlineType;

public record OutlineRegionProvider(OutlineType outlineType, RegionProvider<?> region) implements RegionProvider<OutlineRegion> {
  public static final MapCodec<OutlineRegionProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(OutlineType.OUTLINE_TYPE_FIELD.forGetter(OutlineRegionProvider::outlineType), RegionProvider.CODEC.fieldOf("region").forGetter(OutlineRegionProvider::region)).apply(i, OutlineRegionProvider::new));

  @Override
  public OutlineRegion toAbsoluteRegion(ExecutionContext context) {
    return new OutlineRegion(outlineType, region.toAbsoluteRegion(context));
  }

  @Override
  public RegionType<? super OutlineRegion> getType() {
    return RegionTypes.OUTLINE;
  }

  @Override
  public String expressAsString() {
    return "outline(" + region.expressAsString() + ", " + outlineType.getSerializedName() + ")";
  }
}
