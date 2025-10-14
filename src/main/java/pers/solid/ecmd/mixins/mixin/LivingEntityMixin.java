package pers.solid.ecmd.mixins.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.config.DebugConfig;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

  /**
   * 启用了 {@link DebugConfig#immuneToVoid} 时，免疫虚空伤害。
   *
   * @see DebugConfig#immuneToVoid
   */
  @Inject(method = "tickInVoid", at = @At("HEAD"), cancellable = true)
  private void ignoreVoidTick(CallbackInfo ci) {
    final int immuneToVoid = DebugConfig.current.immuneToVoid;
    switch (immuneToVoid) {
      case 1 -> {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity.isInvulnerable()) ci.cancel();
      }
      case 2 -> {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof PlayerEntity) ci.cancel();
      }
      case 3, 4 -> ci.cancel();
    }
  }
}
