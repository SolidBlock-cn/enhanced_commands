package pers.solid.ecmd.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.regionselection.WandEvent;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
  @Shadow
  @Final
  private MinecraftClient client;

  @Shadow
  private GameMode gameMode;

  @Inject(method = "updateBlockBreakingProgress", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameMode;isCreative()Z", ordinal = 0, shift = At.Shift.BEFORE), cancellable = true)
  public void suspendsUpdatingWand(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
    // 当玩家手持区域选择工具时，阻止其通过此方法调用 AttackBlockCallback
    // 参见 WandEvent
    if (gameMode != GameMode.SPECTATOR && client.player != null && WandEvent.isWand(client.player.getMainHandStack())) {
      cir.setReturnValue(false);
    }
  }
}
