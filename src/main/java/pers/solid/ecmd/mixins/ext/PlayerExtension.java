package pers.solid.ecmd.mixins.ext;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.mixin.PlayerMixin;
import pers.solid.ecmd.regionselection.RegionSelection;

/**
 * 此接口将通过 {@link PlayerMixin} 使 {@link Player} 实现此接口。
 */
public interface PlayerExtension {
  /**
   * 获取玩家的活动区域。此方法将从 dataTracker 中获取。
   */
  @Contract(pure = true)
  @Nullable
  default RegionSelection getActiveRegion$ec() {
    throw new UnsupportedOperationException();
  }

  /**
   * 设置玩家的活动区域。由于是 dataTracker 中设置的，因此在服务器设置后将同步进行更新，除非设置前和设置后是同一个对象。
   */
  default void setActiveRegion$ec(@Nullable RegionSelection region) {
    throw new UnsupportedOperationException();
  }
}
