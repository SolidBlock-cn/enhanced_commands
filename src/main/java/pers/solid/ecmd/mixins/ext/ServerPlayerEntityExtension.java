package pers.solid.ecmd.mixins.ext;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.mixins.mixin.ServerPlayerEntityMixin;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.regionselection.RegionSelectionType;

/**
 * 此接口将通过 {@link ServerPlayerEntityMixin} 使 {@link ServerPlayerEntity} 实现此接口。
 */
public interface ServerPlayerEntityExtension extends PlayerEntityExtension {
  /**
   * 玩家没有活动区域时抛出的异常。
   */
  DynamicCommandExceptionType PLAYER_HAS_NO_ACTIVE_REGION = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.region.no_active_region", o));

  /**
   * 获取玩家的活动区域，如果玩家不存在活动区域，则抛出异常。此方法通常用在必须要玩家拥有活动区域的命令中。
   */
  default @NotNull RegionSelection getActiveRegionOrThrow$ec() throws CommandSyntaxException {
    final RegionSelection region = getActiveRegion$ec();
    if (region == null) {
      throw PLAYER_HAS_NO_ACTIVE_REGION.create(((ServerPlayerEntity) this).getName());
    }
    return region;
  }

  /**
   * 将玩家的活动区域同步至客户端。注意：在执行 {@link #setActiveRegion$ec} 时会自动进行同步，除非设置的是和之前同一个对象。如果修改了 {@link RegionSelection} 自身而没有将玩家的活动区域设置为另一个对象，则需调用此方法。
   */
  default void syncActiveRegion$ec() {
    throw new UnsupportedOperationException();
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
