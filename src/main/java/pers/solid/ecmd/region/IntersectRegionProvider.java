package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.List;
import java.util.stream.Collectors;

public record IntersectRegionProvider(List<RegionProvider<?>> regions) implements RegionProvider<IntersectRegion> {
  public static final MapCodec<IntersectRegionProvider> CODEC = RegionProvider.CODEC.listOf().optionalFieldOf("regions", List.of()).xmap(IntersectRegionProvider::new, IntersectRegionProvider::regions);

  @Override
  public IntersectRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new IntersectRegion(IterateUtils.transformFailableImmutableList(regions, regionArgument -> regionArgument.toAbsoluteRegion(positionProvider)));
  }

  @Override
  public RegionType<IntersectRegion> getType() {
    return RegionTypes.INTERSECT;
  }

  @Override
  public String expressAsString() {
    return "intersect(" + regions.stream().map(ExpressionConvertible::expressAsString).collect(Collectors.joining(", ")) + ")";
  }
}
