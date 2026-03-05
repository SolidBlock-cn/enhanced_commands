package pers.solid.ecmd.util.extension;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.mixins.general.ServerPlayerMixin;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.regionselection.RegionSelectionType;

/**
 * 此接口将通过 {@link ServerPlayerMixin} 使 {@link ServerPlayer} 实现此接口。
 */
public interface ServerPlayerExtension extends PlayerExtension {
  /**
   * 玩家没有活动区域时抛出的异常。
   */
  DynamicCommandExceptionType PLAYER_HAS_NO_ACTIVE_REGION = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.region.no_active_region", o));

  /**
   * 获取玩家的活动区域，如果玩家不存在活动区域，则抛出异常。此方法通常用在必须要玩家拥有活动区域的命令中。
   */
  default @NotNull RegionSelection getActiveRegionOrThrow$ec() throws CommandSyntaxException {
    final RegionSelection region = getActiveRegion$ec();
    if (region == null) {
      throw PLAYER_HAS_NO_ACTIVE_REGION.create(((ServerPlayer) this).getName());
    }
    return region;
  }

  default RegionSelection getOrResetRegionSelection$ec() {
    final RegionSelection region = getActiveRegion$ec();
    if (region != null) {
      return region;
    } else {
      RegionSelection regionSelection = getRegionSelectionType$ec().createRegionSelection();
      setActiveRegion$ec(regionSelection);
      return regionSelection;
    }
  }

  default RegionSelectionType getRegionSelectionType$ec() {
    throw new UnsupportedOperationException();
  }

  default void setRegionSelectionType$ec(RegionSelectionType regionSelectionType) {
    throw new UnsupportedOperationException();
  }

  default void switchRegionSelectionType$ec(RegionSelectionType regionSelectionType) {
    final RegionSelection activeRegion = getActiveRegion$ec();
    if (activeRegion != null) {
      setActiveRegion$ec(regionSelectionType.createRegionSelectionFrom(activeRegion));
    }
    setRegionSelectionType$ec(regionSelectionType);
  }
}
