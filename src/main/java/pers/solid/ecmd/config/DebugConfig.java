package pers.solid.ecmd.config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.mixins.general.*;

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
   * @see pers.solid.ecmd.mixins.general.EntityMixin#noClampWhenUpdating(double, double, double, Operation)
   */
  public boolean ignoreBoundary = false;
  /**
   * 忽视世界边界。
   *
   * @see GuiMixin#skipBorderWarning(double)
   * @see pers.solid.ecmd.mixins.general.WorldBorderMixin
   */
  public boolean ignoreBorder = false;

  /**
   * 免疫虚空伤害。
   *
   * @see pers.solid.ecmd.mixins.general.LivingEntityMixin#ignoreBelowWorld
   * @see pers.solid.ecmd.mixins.general.EntityMixin#ignoreBelowWorld
   */
  public int immuneToVoid = 0;
  /**
   * 即使玩家处于较低的地方，仍正常渲染天空，包括下方的天空和雾。
   *
   * @see LevelRendererMixin#neverRenderDarkDisc
   * @see FogRendererMixin#noDarkFogColor
   */
  public boolean noDarkSky = false;

  /**
   * 玩家没有物理效果，不会受任何碰撞箱影响。可能有部分异常现象。
   */
  public boolean ghostPlayers = false;
  /**
   * 玩家卡在不透明方块内部时，不会被卡视野。
   */
  public boolean clearVisionInsideBlocks = false;
  /**
   * 所有的玩家都没有碰撞箱，可任意穿过方块，但仍会正常受到流体的影响。此选项不影响在不透明方块内部时的卡视野，也不影响窒息伤害。注意：未飞行的玩家会坠入虚空。
   */
  public boolean playersNoCollision = false;
  /**
   * 所有的实体都没有碰撞箱，可任意穿过方块，但仍会正常受到液体的影响。此选项不影响窒息伤害。注意：未飞行的实体会坠入虚空。
   */
  public boolean entitiesNoCollision = false;
  /**
   * 当玩家有部分位置与方块碰撞箱有重叠时，不再自动弹至有空间的位置。
   */
  public boolean disableAutoPositionAdjustment = false;

  @Override
  public DebugConfig clone() {
    try {
      return (DebugConfig) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
