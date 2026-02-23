package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.mixins.ext.EntitySelectorExtension;
import pers.solid.ecmd.predicate.entity.EntitySelectorCollector;

import java.util.List;
import java.util.function.Predicate;

@Mixin(EntitySelector.class)
public abstract class EntitySelectorMixin implements EntitySelectorExtension {
  @Shadow
  protected abstract Predicate<Entity> getPredicate(Vec3 pos, @Nullable AABB box, @Nullable FeatureFlagSet enabledFeatures);

  @Inject(method = {"findSingleEntity", "findEntities(Lnet/minecraft/commands/CommandSourceStack;)Ljava/util/List;", "findSinglePlayer", "findPlayers"}, at = @At("HEAD"))
  private void setSource(CommandSourceStack source, CallbackInfoReturnable<Entity> cir) {
    extension$ec().updateSource(source);
  }

  /**
   * 特定类型的实体选择器（如 {@code passengers}）应该以特殊的方式，从世界收集实体列表。
   */
  @Inject(method = "findEntities(Lnet/minecraft/commands/CommandSourceStack;)Ljava/util/List;", at = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/selector/EntitySelector;currentEntity:Z"), cancellable = true)
  private void modifiedEntityCollector(CommandSourceStack source, CallbackInfoReturnable<List<? extends Entity>> cir, @Local Vec3 vec3d, @Local AABB box) throws CommandSyntaxException {
    final EntitySelectorCollector collector = extension$ec().collector;
    if (collector != null) {
      final EntitySelector collectorOf = extension$ec().collectorOf;
      if (collectorOf == null) {
        cir.setReturnValue(collector.collectEntities(source.getEntityOrException()).filter(getPredicate(vec3d, box, null)).toList());
      } else {
        cir.setReturnValue(collectorOf.findEntities(source).stream().flatMap(collector::collectEntities).toList());
      }
    }
  }

  /**
   * 特定类型的实体选择器（如 {@code passengers}）应该以特殊的方式，从世界收集玩家列表。
   */
  @Inject(method = "findPlayers", at = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/selector/EntitySelector;currentEntity:Z"), cancellable = true)
  private void modifiedPlayerCollector(CommandSourceStack source, CallbackInfoReturnable<List<ServerPlayer>> cir, @Local Predicate<Entity> actualPredicate) throws CommandSyntaxException {
    final EntitySelectorCollector collector = extension$ec().collector;
    if (collector != null) {
      final EntitySelector collectorOf = extension$ec().collectorOf;
      if (collectorOf == null) {
        cir.setReturnValue(collector.collectPlayers(source.getEntityOrException()).filter(actualPredicate).toList());
      } else {
        cir.setReturnValue(collectorOf.findEntities(source).stream().flatMap(collector::collectPlayers).toList());
      }
    }
  }
}
