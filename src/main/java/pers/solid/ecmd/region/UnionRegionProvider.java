package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.List;
import java.util.stream.Collectors;

public record UnionRegionProvider(List<RegionProvider<?>> regions) implements RegionProvider<UnionRegion> {
  public static final MapCodec<UnionRegionProvider> CODEC = RegionProvider.CODEC.listOf().optionalFieldOf("regions", List.of()).xmap(UnionRegionProvider::new, UnionRegionProvider::regions);

  @Override
  public UnionRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new UnionRegion(IterateUtils.transformFailableImmutableList(regions, regionArgument -> regionArgument.toAbsoluteRegion(positionProvider)));
  }

  @Override
  public RegionType<UnionRegion> getType() {
    return RegionTypes.UNION;
  }

  @Override
  public String expressAsString() {
    return "union(" + regions.stream().map(ExpressionConvertible::expressAsString).collect(Collectors.joining(", ")) + ")";
  }
}
