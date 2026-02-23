package pers.solid.ecmd.mixins.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.regionselection.WandEvent;

@Environment(EnvType.CLIENT)
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
  @Shadow
  @Final
  private Minecraft minecraft;

  @Shadow
  private GameType localPlayerMode;

  @Inject(method = "continueDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameType;isCreative()Z", ordinal = 0), cancellable = true, order = 500)
  public void suspendsUpdatingWand(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
    // 当玩家手持区域选择工具时，阻止其通过此方法调用 AttackBlockCallback
    // 参见 WandEvent
    if (localPlayerMode != GameType.SPECTATOR && minecraft.player != null && WandEvent.isWand(minecraft.player.getMainHandItem())) {
      cir.setReturnValue(false);
    }
  }
}
