package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.command.argument.RegistryEntryPredicateArgumentType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.command.LocateCommand;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.argument.EnhancedEntryPredicate;

import java.util.function.Function;

@Mixin(LocateCommand.class)
public abstract class LocateCommandMixin {
  @Shadow
  private static String getKeyString(Pair<BlockPos, ? extends RegistryEntry<?>> result) {
    return null;
  }

  /**
   * 此方法用于对 {@link EnhancedEntryPredicate.AnyOf} 进行特殊处理，因为其返回的 {@link EnhancedEntryPredicate.AnyOf#getEntry()} 方法无法正常使用，需要特殊处理，故使用共享值并返回 null。
   */
  @WrapOperation(method = "sendCoordinates(Lnet/minecraft/server/command/ServerCommandSource;Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryPredicate;Lnet/minecraft/util/math/BlockPos;Lcom/mojang/datafixers/util/Pair;Ljava/lang/String;ZLjava/time/Duration;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryPredicate;getEntry()Lcom/mojang/datafixers/util/Either;"))
  private static <T> Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> wrappedGetEntry(RegistryEntryPredicateArgumentType.EntryPredicate<T> instance, Operation<Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>>> original, @Share("special_predicate") LocalRef<EnhancedEntryPredicate.AnyOf<T>> anyOfShare) {
    if (instance instanceof EnhancedEntryPredicate.AnyOf<T> anyOf) {
      anyOfShare.set(anyOf);
      return null;
    } else {
      anyOfShare.set(null);
      return original.call(instance);
    }
  }

  /**
   * 此方法用于对 {@link EnhancedEntryPredicate.AnyOf} 进行特殊处理，因为其返回的 {@link EnhancedEntryPredicate.AnyOf#getEntry()} 方法无法正常使用，需要特殊处理，故使用共享值并返回特殊的值。
   */
  @WrapOperation(method = "sendCoordinates(Lnet/minecraft/server/command/ServerCommandSource;Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryPredicate;Lnet/minecraft/util/math/BlockPos;Lcom/mojang/datafixers/util/Pair;Ljava/lang/String;ZLjava/time/Duration;)I", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Either;map(Ljava/util/function/Function;Ljava/util/function/Function;)Ljava/lang/Object;"))
  private static <T> Object wrappedMap(Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> instance, Function<? super RegistryEntry.Reference<T>, ? extends T> l, Function<? super RegistryEntryList.Named<T>, ? extends T> r, Operation<String> original, @Share("special_predicate") LocalRef<EnhancedEntryPredicate.AnyOf<T>> anyOfShare, @Local(argsOnly = true) Pair<BlockPos, ? extends RegistryEntry<?>> result) {
    final EnhancedEntryPredicate.AnyOf<T> get = anyOfShare.get();
    if (get != null) {
      return get.asString() + " (" + getKeyString(result) + ")";
    } else {
      return original.call(instance, l, r);
    }
  }
}
