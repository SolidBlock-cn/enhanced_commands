package pers.solid.ecmd.mixins.general;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.config.DebugConfig;
import pers.solid.ecmd.config.GameplayConfig;

@Environment(EnvType.CLIENT)
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {
  public LocalPlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
    super(clientLevel, gameProfile);
  }

  @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
  private void doNotMoveTowardsClosestSpace(double x, double z, CallbackInfo ci) {
    if (DebugConfig.current.playersNoCollision || DebugConfig.current.entitiesNoCollision || DebugConfig.current.disableAutoPositionAdjustment) {
      ci.cancel();
    }
    if (GameplayConfig.current.flyThroughBlocks && this.getAbilities().flying) {
      ci.cancel();
    }
  }
}
