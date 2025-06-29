package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.List;
import java.util.stream.Collectors;

public record UnionRegionArgument(@NotNull List<RegionArgument<?>> regions) implements RegionArgument<UnionRegion> {
  public static final MapCodec<UnionRegionArgument> CODEC = RegionArgument.CODEC.listOf().optionalFieldOf("regions", List.of()).xmap(UnionRegionArgument::new, UnionRegionArgument::regions);

  @Override
  public UnionRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new UnionRegion(IterateUtils.transformFailableImmutableList(regions, regionArgument -> regionArgument.toAbsoluteRegion(positionProvider)));
  }

  @Override
  public @NotNull RegionType<UnionRegion> getType() {
    return RegionTypes.UNION;
  }

  @Override
  public @NotNull String asString() {
    return "union(" + regions.stream().map(ExpressionConvertible::asString).collect(Collectors.joining(", ")) + ")";
  }
}
