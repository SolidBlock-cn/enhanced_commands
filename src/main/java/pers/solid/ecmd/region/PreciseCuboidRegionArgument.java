package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;

public record PreciseCuboidRegionArgument(EnhancedPosArgument from, EnhancedPosArgument to) implements CuboidRegionArgument<PreciseCuboidRegion> {
  public static final MapCodec<PreciseCuboidRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnhancedPosArgument.CODEC.fieldOf("from").forGetter(PreciseCuboidRegionArgument::from),
      EnhancedPosArgument.CODEC.fieldOf("to").forGetter(PreciseCuboidRegionArgument::to)
  ).apply(i, PreciseCuboidRegionArgument::new));

  @Override
  public PreciseCuboidRegion toAbsoluteRegion(ServerCommandSource source) throws CommandSyntaxException {
    return new PreciseCuboidRegion(from.toAbsolutePos(source), to.toAbsolutePos(source));
  }

  @Override
  public @NotNull RegionType<? super PreciseCuboidRegion> getType() {
    return RegionTypes.CUBOID;
  }

  @Override
  public @NotNull String asString() {
    return "cuboid(" + from.asString() + ", " + to.asString() + ")";
  }
}
