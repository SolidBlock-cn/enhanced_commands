package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

public record CuboidOutlineRegionArgument(BlockCuboidRegionArgument region, int thickness) implements RegionArgument<CuboidOutlineRegion> {
  public static final MapCodec<CuboidOutlineRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockCuboidRegionArgument.CODEC.fieldOf("region").forGetter(CuboidOutlineRegionArgument::region), Codec.INT.optionalFieldOf("thickness", 1).forGetter(CuboidOutlineRegionArgument::thickness)).apply(i, CuboidOutlineRegionArgument::new));

  @Override
  public CuboidOutlineRegion toAbsoluteRegion(ServerCommandSource source) throws CommandSyntaxException {
    return new CuboidOutlineRegion(region.toAbsoluteRegion(source), thickness);
  }

  @Override
  public @NotNull RegionType<? super CuboidOutlineRegion> getType() {
    return RegionTypes.CUBOID_OUTLINE;
  }

  @Override
  public @NotNull String asString() {
    return "cuboid_outline(" + region.from().asString() + ", " + region.to().asString() + ", " + thickness + ")";
  }
}
