package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.EnhancedCommandsDataAttachments;
import pers.solid.ecmd.config.DebugConfig;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.util.extension.PlayerExtension;

@Mixin(Player.class)
public abstract class PlayerMixin implements PlayerExtension {
  /**
   * 当 ignoreBoundary 设置为 true 时，允许玩家传送到世界界限以外，也就是不要执行 setPosition。
   *
   * @see DebugConfig#ignoreBoundary
   */
  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setPos(DDD)V"))
  private boolean noClampPos(Player instance, double x, double y, double z) {
    return !DebugConfig.current.ignoreBoundary;
  }


  @Override
  public @Nullable RegionSelection getActiveRegion$ec() {
    return EnhancedCommandsDataAttachments.getActiveRegionForPlayer((Player) (Object) this);
  }

  @Override
  public void setActiveRegion$ec(@Nullable RegionSelection region) {
    EnhancedCommandsDataAttachments.setActiveRegionForPlayer((Player) (Object) this, region);
  }
}
