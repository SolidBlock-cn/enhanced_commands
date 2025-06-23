package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;

public record OutwardsRegionArgument(EnhancedPosArgument center, int x, int y, int z) implements RegionArgument<OutwardsRegion> {
  public static final MapCodec<OutwardsRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(EnhancedPosArgument.CODEC.fieldOf("center").forGetter(OutwardsRegionArgument::center), Codec.INT.fieldOf("x").forGetter(OutwardsRegionArgument::x), Codec.INT.fieldOf("y").forGetter(OutwardsRegionArgument::y), Codec.INT.fieldOf("z").forGetter(OutwardsRegionArgument::z)).apply(i, OutwardsRegionArgument::new));

  @Override
  public OutwardsRegion toAbsoluteRegion(ServerCommandSource source) throws CommandSyntaxException {
    return new OutwardsRegion(center.toAbsoluteBlockPos(source), x, y, z);
  }

  @Override
  public @NotNull RegionType<OutwardsRegion> getType() {
    return RegionTypes.OUTWARDS;
  }

  @Override
  public @NotNull String asString() {
    return "outwards(" + center.asString() + ", " + x + " " + y + " " + z + ")";
  }
}
