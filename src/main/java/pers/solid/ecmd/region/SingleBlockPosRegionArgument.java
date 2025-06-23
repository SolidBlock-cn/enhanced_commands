package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;

public record SingleBlockPosRegionArgument(EnhancedPosArgument pos) implements CuboidRegionArgument<SingleBlockPosRegion> {
  public static final MapCodec<SingleBlockPosRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(EnhancedPosArgument.CODEC.fieldOf("pos").forGetter(SingleBlockPosRegionArgument::pos)).apply(i, SingleBlockPosRegionArgument::new));

  @Override
  public SingleBlockPosRegion toAbsoluteRegion(ServerCommandSource source) throws CommandSyntaxException {
    return new SingleBlockPosRegion(pos.toAbsoluteBlockPos(source));
  }

  @Override
  public @NotNull RegionType<SingleBlockPosRegion> getType() {
    return RegionTypes.SINGLE;
  }

  @Override
  public @NotNull String asString() {
    return "single(" + pos.asString() + ")";
  }
}
