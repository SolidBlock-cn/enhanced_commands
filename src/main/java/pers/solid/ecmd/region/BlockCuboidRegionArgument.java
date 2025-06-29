package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.util.PositionProvider;

public record BlockCuboidRegionArgument(EnhancedPosArgument from, EnhancedPosArgument to) implements CuboidRegionArgument<BlockCuboidRegion> {
  public static final MapCodec<BlockCuboidRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnhancedPosArgument.CODEC.fieldOf("from").forGetter(BlockCuboidRegionArgument::from),
      EnhancedPosArgument.CODEC.fieldOf("to").forGetter(BlockCuboidRegionArgument::to)
  ).apply(i, BlockCuboidRegionArgument::new));

  @Override
  public BlockCuboidRegion toAbsoluteRegion(PositionProvider positionProvider) {
    return new BlockCuboidRegion(from.toAbsoluteBlockPos(positionProvider), to.toAbsoluteBlockPos(positionProvider));
  }

  @Override
  public @NotNull RegionType<CuboidRegion> getType() {
    return RegionTypes.CUBOID;
  }

  @Override
  public @NotNull String asString() {
    return "cuboid(" + from.asString() + ", " + to.asString() + ")";
  }
}
