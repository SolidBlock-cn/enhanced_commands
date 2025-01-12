package pers.solid.ecmd.mixins.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SharedConstants.class)
public class SharedConstantsTempMixin {
  @Inject(method = "<clinit>", at = @At("HEAD"))
  private static void injHead(CallbackInfo ci) {
    LogUtils.getLogger().info("Init SharedConstants head!", new Throwable("test throwable"));
  }

  @Inject(method = "<clinit>", at = @At(value = "INVOKE", target = "Lio/netty/util/ResourceLeakDetector;setLevel(Lio/netty/util/ResourceLeakDetector$Level;)V", shift = At.Shift.BEFORE, remap = false))
  private static void injBeforesetLevel(CallbackInfo ci) {
    LogUtils.getLogger().info("Init before set level!", new Throwable("test throwable"));
  }

  @Inject(method = "<clinit>", at = @At(value = "NEW", target = "()Lnet/minecraft/command/TranslatableBuiltInExceptions;", shift = At.Shift.BEFORE))
  private static void injBeforeTranslateNew(CallbackInfo ci) {
    LogUtils.getLogger().info("Init before translate builtin new!", new Throwable("test throwable"));
  }

  @Inject(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/TranslatableBuiltInExceptions;<init>()V", shift = At.Shift.BEFORE))
  private static void injBeforeTranslateInit(CallbackInfo ci) {
    LogUtils.getLogger().info("Init before translate builtin init!", new Throwable("test throwable"));
  }

  @Inject(method = "<clinit>", at = @At("TAIL"))
  private static void injTail(CallbackInfo ci) {
    LogUtils.getLogger().info("Init SharedConstants tail!", new Throwable("test throwable"));
  }
}
