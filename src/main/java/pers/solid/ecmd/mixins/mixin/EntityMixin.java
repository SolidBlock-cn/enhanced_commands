package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.config.DebugConfig;

@Mixin(Entity.class)
public abstract class EntityMixin {

  /**
   * 当 ignoreBoundary 为 true 时，更新玩家坐标时，不受世界界限的限制。
   *
   * @see DebugConfig#ignoreBoundary
   */
  @WrapOperation(method = "updatePosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(DDD)D"))
  private double noClampWhenUpdating(double value, double min, double max, Operation<Double> original) {
    if (DebugConfig.current.ignoreBoundary) {
      return value;
    } else {
      return original.call(value, min, max);
    }
  }

  @Inject(method = "tickInVoid", at = @At("HEAD"), cancellable = true)
  private void ignoreVoidTick(CallbackInfo ci) {
    if (DebugConfig.current.immuneToVoid == 4) {
      ci.cancel();
    }
  }
}
