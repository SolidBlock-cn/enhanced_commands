package pers.solid.ecmd.mixins.mixin;

import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.util.mixin.MixinShared;

@Mixin(GameType.class)
public abstract class GameTypeMixin {
  /**
   * 修改此方法以允许 {@link GameType#byName(String, GameType)} 接受本模组中自定义的游戏模式名称。
   */
  @Inject(method = "byName(Ljava/lang/String;Lnet/minecraft/world/level/GameType;)Lnet/minecraft/world/level/GameType;", at = @At("HEAD"), cancellable = true)
  private static void acceptAdditionalNames(String name, GameType defaultMode, CallbackInfoReturnable<GameType> cir) {
    if (MixinShared.EXTENDED_GAME_MODE_NAMES.containsKey(name)) {
      cir.setReturnValue(MixinShared.EXTENDED_GAME_MODE_NAMES.get(name));
    }
  }
}
