package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.command.DebugIgnoreBoundaryCommand;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
  /**
   * 当 ignoreBoundary 设置为 true 时，允许玩家传送到世界界限以外，也就是不要执行 setPosition。
   *
   * @see DebugIgnoreBoundaryCommand#ignoreBoundary
   */
  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setPosition(DDD)V"))
  private boolean noClampPos(PlayerEntity instance, double x, double y, double z) {
    return !DebugIgnoreBoundaryCommand.ignoreBoundary;
  }
}
