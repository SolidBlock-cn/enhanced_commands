package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

public enum ActiveRegionArgument implements RegionArgument<Region> {
  INSTANCE;

  public static final MapCodec<ActiveRegionArgument> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public Region toAbsoluteRegion(ServerCommandSource source) throws CommandSyntaxException {
    return ((ServerPlayerEntityExtension) source.getPlayerOrThrow()).ec$getOrEvaluateActiveRegionOrThrow();
  }

  @Override
  public @NotNull ActiveRegionType getType() {
    return RegionTypes.ACTIVE_REGION;
  }

  @Override
  public @NotNull String asString() {
    return "$";
  }
}
