package pers.solid.ecmd.config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.mixins.mixin.*;

/**
 * 本模组中用于调试的一些配置项。
 */
public class DebugConfig implements Cloneable {
  public static final DebugConfig DEFAULT = new DebugConfig();
  public static DebugConfig current = DEFAULT;
  /**
   * 忽略世界界限。
   *
   * @see PlayerMixin#noClampPos(Player, double, double, double)
   * @see ServerGamePacketListenerImplMixin#noClampHorizontal(double, CallbackInfoReturnable)
   * @see ServerGamePacketListenerImplMixin#noClampVertical(double, CallbackInfoReturnable)
   * @see LevelMixin#forceValidHorizontally(BlockPos, CallbackInfoReturnable)
   * @see LevelMixin#forceValidVertically(int, CallbackInfoReturnable)
   * @see pers.solid.ecmd.mixins.mixin.EntityMixin#noClampWhenUpdating(double, double, double, Operation)
   */
  public boolean ignoreBoundary = false;
  /**
   * 忽视世界边界。
   *
   * @see GuiMixin#skipBorderWarning(double)
   * @see pers.solid.ecmd.mixins.mixin.WorldBorderMixin
   */
  public boolean ignoreBorder = false;

  /**
   * 免疫虚空伤害。
   *
   * @see pers.solid.ecmd.mixins.mixin.LivingEntityMixin#ignoreBelowWorld
   * @see pers.solid.ecmd.mixins.mixin.EntityMixin#ignoreBelowWorld
   */
  public int immuneToVoid = 0;
  /**
   * 即使玩家处于较低的地方，仍正常渲染天空，包括下方的天空和雾。
   *
   * @see LevelRendererMixin#neverRenderDarkDisc
   * @see FogRendererMixin#noDarkFogColor
   */
  public boolean noDarkSky = false;

  @Override
  public DebugConfig clone() {
    try {
      return (DebugConfig) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
