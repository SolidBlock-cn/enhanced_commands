package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.mixins.ext.EntitySelectorExtension;

import java.util.List;
import java.util.function.Predicate;

@Mixin(EntitySelector.class)
public abstract class EntitySelectorMixin implements EntitySelectorExtension {
  @Shadow
  protected abstract Predicate<Entity> getPositionPredicate(Vec3d pos, @Nullable Box box, @Nullable FeatureSet enabledFeatures);

  @Inject(method = {"getEntity", "getEntities(Lnet/minecraft/server/command/ServerCommandSource;)Ljava/util/List;", "getPlayer", "getPlayers"}, at = @At("HEAD"))
  private void setSource(ServerCommandSource source, CallbackInfoReturnable<Entity> cir) {
    extension$ec().updateSource(source);
  }

  /**
   * 特定类型的实体选择器（如 {@code passengers}）应该以特殊的方式，从世界收集实体列表。
   */
  @Inject(method = "getEntities(Lnet/minecraft/server/command/ServerCommandSource;)Ljava/util/List;", at = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelector;senderOnly:Z"), cancellable = true)
  private void modifiedEntityCollector(ServerCommandSource source, CallbackInfoReturnable<List<? extends Entity>> cir, @Local Vec3d vec3d, @Local Box box) throws CommandSyntaxException {
    if (extension$ec().collector != null) {
      cir.setReturnValue(extension$ec().collector.collectEntities(source.getEntityOrThrow()).filter(getPositionPredicate(vec3d, box, null)).toList());
    }
  }

  /**
   * 特定类型的实体选择器（如 {@code passengers}）应该以特殊的方式，从世界收集玩家列表。
   */
  @Inject(method = "getPlayers", at = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelector;senderOnly:Z"), cancellable = true)
  private void modifiedPlayerCollector(ServerCommandSource source, CallbackInfoReturnable<List<ServerPlayerEntity>> cir, @Local Predicate<Entity> actualPredicate) throws CommandSyntaxException {
    if (extension$ec().collector != null) {
      cir.setReturnValue(extension$ec().collector.collectPlayers(source.getEntityOrThrow()).filter(actualPredicate).toList());
    }
  }
}
