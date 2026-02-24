package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.mixins.ext.ServerPlayerExtension;
import pers.solid.ecmd.util.PositionProvider;

public enum ActiveRegionProvider implements RegionProvider<Region> {
  INSTANCE;

  public static final MapCodec<ActiveRegionProvider> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public Region toAbsoluteRegion(PositionProvider positionProvider) {
    try {
      final Player playerEntity = positionProvider.getPlayerOrThrow$ec();
      if (playerEntity instanceof ServerPlayerExtension serverPlayerEntityExtension) {
        return serverPlayerEntityExtension.getActiveRegionOrThrow$ec().region();
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
