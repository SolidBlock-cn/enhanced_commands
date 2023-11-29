package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.predicate.NumberRange;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.predicate.entity.EntitySelectorExtras;
import pers.solid.ecmd.util.mixin.EntitySelectorExtension;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(EntitySelector.class)
public class EntitySelectorMixin implements EntitySelectorExtension {
  @Unique
  private final EntitySelectorExtras ec$ext = new EntitySelectorExtras();

  @Override
  public EntitySelectorExtras ec$getExt() {
    return ec$ext;
  }

  @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelector;basePredicate:Ljava/util/function/Predicate;", shift = At.Shift.BEFORE))
  private void modifyBasePredicate(int count, boolean includesNonPlayers, boolean localWorldOnly, Predicate<Entity> basePredicate, NumberRange.FloatRange distance, Function<Vec3d, Vec3d> positionOffset, @Nullable Box box, BiConsumer<Vec3d, List<? extends Entity>> sorter, boolean senderOnly, @Nullable String playerName, @Nullable UUID uuid, @Nullable EntityType<?> type, boolean usesAt, CallbackInfo ci, @Local LocalRef<Predicate<Entity>> basePredicateRef) {
    basePredicateRef.set(basePredicateRef.get().and(this.ec$ext::testForExtraPredicates));
  }

  @Inject(method = {"getEntity", "getUnfilteredEntities", "getPlayer", "getPlayers"}, at = @At("HEAD"))
  private void setSource(ServerCommandSource source, CallbackInfoReturnable<Entity> cir) throws CommandSyntaxException {
    ec$ext.updateSource(source);
  }
}
