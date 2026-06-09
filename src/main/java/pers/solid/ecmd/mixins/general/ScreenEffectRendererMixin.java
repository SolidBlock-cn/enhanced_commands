package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.config.DebugConfig;
import pers.solid.ecmd.config.GameplayConfig;

@Mixin(ScreenEffectRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class ScreenEffectRendererMixin {
  @ModifyExpressionValue(method = "renderScreenEffect", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;noPhysics:Z", opcode = Opcodes.GETFIELD))
  private static boolean noViewBlockingState(boolean original, @Local Player player) {
    return DebugConfig.current.clearVisionInsideBlocks || (GameplayConfig.current.flyThroughBlocks && player.getAbilities().flying) || original;
  }
}
