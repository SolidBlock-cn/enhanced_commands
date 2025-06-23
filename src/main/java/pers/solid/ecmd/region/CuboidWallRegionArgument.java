package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

public record CuboidWallRegionArgument(BlockCuboidRegionArgument region, int thickness) implements RegionArgument<CuboidWallRegion> {
  public static final MapCodec<CuboidWallRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockCuboidRegionArgument.CODEC.fieldOf("region").forGetter(CuboidWallRegionArgument::region), Codec.INT.optionalFieldOf("thickness", 1).forGetter(CuboidWallRegionArgument::thickness)).apply(i, CuboidWallRegionArgument::new));

  @Override
  public CuboidWallRegion toAbsoluteRegion(ServerCommandSource source) throws CommandSyntaxException {
    return new CuboidWallRegion(region.toAbsoluteRegion(source), thickness);
  }

  @Override
  public @NotNull RegionType<? super CuboidWallRegion> getType() {
    return RegionTypes.CUBOID_WALL;
  }

  @Override
  public @NotNull String asString() {
    return "cuboid_wall(" + region.from().asString() + ", " + region.to().asString() + ", " + thickness + ")";
  }
}
