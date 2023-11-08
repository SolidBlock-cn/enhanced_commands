package pers.solid.ecmd.mixin;

import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.util.mixin.MixinSharedVariables;

@Mixin(GameMode.class)
public abstract class GameModeMixin {
  /**
   * 修改此方法以允许 {@link GameMode#byName(String, GameMode)} 接受本模组中自定义的游戏模式名称。
   */
  @Inject(method = "byName(Ljava/lang/String;Lnet/minecraft/world/GameMode;)Lnet/minecraft/world/GameMode;", at = @At("HEAD"), cancellable = true)
  private static void acceptAdditionalNames(String name, GameMode defaultMode, CallbackInfoReturnable<GameMode> cir) {
    if (MixinSharedVariables.EXTENDED_GAME_MODE_NAMES.containsKey(name)) {
      cir.setReturnValue(MixinSharedVariables.EXTENDED_GAME_MODE_NAMES.get(name));
    }
  }
}
