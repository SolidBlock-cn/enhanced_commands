package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;

public record BlockCuboidRegionArgument(EnhancedPosArgument from, EnhancedPosArgument to) implements CuboidRegionArgument<BlockCuboidRegion> {
  public static final MapCodec<BlockCuboidRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnhancedPosArgument.CODEC.fieldOf("from").forGetter(BlockCuboidRegionArgument::from),
      EnhancedPosArgument.CODEC.fieldOf("to").forGetter(BlockCuboidRegionArgument::to)
  ).apply(i, BlockCuboidRegionArgument::new));

  @Override
  public BlockCuboidRegion toAbsoluteRegion(ServerCommandSource source) throws CommandSyntaxException {
    return new BlockCuboidRegion(from.toAbsoluteBlockPos(source), to.toAbsoluteBlockPos(source));
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
