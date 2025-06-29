package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

public enum ActiveRegionArgument implements RegionArgument<Region> {
  INSTANCE;

  public static final MapCodec<ActiveRegionArgument> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public Region toAbsoluteRegion(PositionProvider positionProvider) {
    try {
      final PlayerEntity playerEntity = positionProvider.playerOrThrow$ec();
      if (playerEntity instanceof ServerPlayerEntityExtension serverPlayerEntityExtension) {
        return serverPlayerEntityExtension.ec$getActiveRegion();
      } else {
        throw new CommandRuntimeException();
      }
    } catch (CommandSyntaxException e) {
      throw new CommandRuntimeException(e);
    }
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
