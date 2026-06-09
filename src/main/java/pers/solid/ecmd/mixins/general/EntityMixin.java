package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.config.DebugConfig;
import pers.solid.ecmd.config.GameplayConfig;

@Mixin(Entity.class)
public abstract class EntityMixin {

  @Shadow
  public int invulnerableTime;

  /**
   * 当 ignoreBoundary 为 true 时，更新玩家坐标时，不受世界界限的限制。
   *
   * @see DebugConfig#ignoreBoundary
   */
  @WrapOperation(method = "absMoveTo(DDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D"))
  private double noClampWhenUpdating(double value, double min, double max, Operation<Double> original) {
    if (DebugConfig.current.ignoreBoundary) {
      return value;
    } else {
      return original.call(value, min, max);
    }
  }

  @Inject(method = "onBelowWorld", at = @At("HEAD"), cancellable = true)
  private void ignoreBelowWorld(CallbackInfo ci) {
    if (DebugConfig.current.immuneToVoid == 4) {
      ci.cancel();
    }
  }

  @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
  private void bypassCollide(Vec3 vec, CallbackInfoReturnable<Vec3> cir) {
    final Entity thisEntity = (Entity) (Object) this;
    if (DebugConfig.current.entitiesNoCollision || (thisEntity instanceof Player && DebugConfig.current.playersNoCollision)) {
      cir.setReturnValue(vec);
    }
    if (thisEntity instanceof Player player && GameplayConfig.current.flyThroughBlocks && player.getAbilities().flying) {
      cir.setReturnValue(vec);
    }
  }
}
