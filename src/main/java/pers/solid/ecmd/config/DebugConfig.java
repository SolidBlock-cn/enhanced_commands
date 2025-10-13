package pers.solid.ecmd.config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class DebugConfig implements Cloneable {
  public static final DebugConfig DEFAULT = new DebugConfig();
  public static DebugConfig current = DEFAULT;
  /**
   * 忽略世界界限。
   *
   * @see pers.solid.ecmd.mixins.mixin.PlayerEntityMixin#noClampPos(PlayerEntity, double, double, double)
   * @see pers.solid.ecmd.mixins.mixin.ServerPlayNetworkHandlerMixin#noClampHorizontal(double, CallbackInfoReturnable)
   * @see pers.solid.ecmd.mixins.mixin.ServerPlayNetworkHandlerMixin#noClampVertical(double, CallbackInfoReturnable)
   * @see pers.solid.ecmd.mixins.mixin.WorldMixin#forceValidHorizontally(BlockPos, CallbackInfoReturnable)
   * @see pers.solid.ecmd.mixins.mixin.WorldMixin#forceValidVertically(int, CallbackInfoReturnable)
   * @see pers.solid.ecmd.mixins.mixin.EntityMixin#noClampWhenUpdating(double, double, double, Operation)
   */
  public boolean ignoreBoundary = false;
  /**
   * 忽视世界边界。
   *
   * @see pers.solid.ecmd.mixins.mixin.InGameHudMixin#skipBorderWarning(double)
   * @see pers.solid.ecmd.mixins.mixin.WorldBorderMixin
   */
  public boolean ignoreBorder = false;

  @Override
  public DebugConfig clone() {
    try {
      return (DebugConfig) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
