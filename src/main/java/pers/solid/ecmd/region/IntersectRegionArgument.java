package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.List;
import java.util.stream.Collectors;

public record IntersectRegionArgument(@NotNull List<RegionArgument<?>> regions) implements RegionArgument<IntersectRegion> {
  public static final MapCodec<IntersectRegionArgument> CODEC = RegionArgument.CODEC.listOf().optionalFieldOf("regions", List.of()).xmap(IntersectRegionArgument::new, IntersectRegionArgument::regions);

  @Override
  public IntersectRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new IntersectRegion(IterateUtils.transformFailableImmutableList(regions, regionArgument -> regionArgument.toAbsoluteRegion(positionProvider)));
  }

  @Override
  public @NotNull RegionType<IntersectRegion> getType() {
    return RegionTypes.INTERSECT;
  }

  @Override
  public @NotNull String asString() {
    return "intersect(" + regions.stream().map(ExpressionConvertible::asString).collect(Collectors.joining(", ")) + ")";
  }
}
