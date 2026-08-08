package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.extension.ServerPlayerExtension;

public enum ActiveRegionProvider implements RegionProvider<Region> {
  INSTANCE;

  public static final MapCodec<ActiveRegionProvider> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public Region toAbsoluteRegion(ExecutionContext context) {
    try {
      final Player playerEntity = context.positionProvider.getPlayerOrThrow$ec();
      if (playerEntity instanceof ServerPlayerExtension serverPlayerEntityExtension) {
        return serverPlayerEntityExtension.getActiveRegionOrThrow$ec().region();
      } else {
        throw new CommandRuntimeException(Component.literal("cannot get active region for non-server player")); // 考虑使用可翻译文本
      }
    } catch (CommandSyntaxException e) {
      throw new CommandRuntimeException(e);
    }
  }

  @Override
  public RegionType<Region> getType() {
    return RegionTypes.ACTIVE_REGION;
  }

  @Override
  public String expressAsString() {
    return "$";
  }
}
