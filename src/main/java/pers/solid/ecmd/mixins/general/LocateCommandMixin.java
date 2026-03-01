package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.commands.LocateCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.argument.EnhancedEntryPredicate;

import java.util.function.Function;

@Mixin(LocateCommand.class)
public abstract class LocateCommandMixin {

  /**
   * 此方法用于对 {@link EnhancedEntryPredicate.AnyOf} 进行特殊处理，因为其返回的 {@link EnhancedEntryPredicate.AnyOf#unwrap()} 方法无法正常使用，需要特殊处理，故使用共享值并返回 null。
   */
  @WrapOperation(method = "showLocateResult(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/commands/arguments/ResourceOrTagArgument$Result;Lnet/minecraft/core/BlockPos;Lcom/mojang/datafixers/util/Pair;Ljava/lang/String;ZLjava/time/Duration;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/ResourceOrTagArgument$Result;unwrap()Lcom/mojang/datafixers/util/Either;"))
  private static <T> Either<Holder.Reference<T>, HolderSet.Named<T>> wrappedGetEntry(ResourceOrTagArgument.Result<T> instance, Operation<Either<Holder.Reference<T>, HolderSet.Named<T>>> original, @Share("special_predicate") LocalRef<EnhancedEntryPredicate.AnyOf<T>> anyOfShare) {
    if (instance instanceof EnhancedEntryPredicate.AnyOf<T> anyOf) {
      anyOfShare.set(anyOf);
      return null;
    } else {
      anyOfShare.set(null);
      return original.call(instance);
    }
  }

  /**
   * 此方法用于对 {@link EnhancedEntryPredicate.AnyOf} 进行特殊处理，因为其返回的 {@link EnhancedEntryPredicate.AnyOf#unwrap()} 方法无法正常使用，需要特殊处理，故使用共享值并返回特殊的值。
   */
  @WrapOperation(method = "showLocateResult(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/commands/arguments/ResourceOrTagArgument$Result;Lnet/minecraft/core/BlockPos;Lcom/mojang/datafixers/util/Pair;Ljava/lang/String;ZLjava/time/Duration;)I", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Either;map(Ljava/util/function/Function;Ljava/util/function/Function;)Ljava/lang/Object;", remap = false))
  private static <T> Object wrappedMap(Either<Holder.Reference<T>, HolderSet.Named<T>> instance, Function<? super Holder.Reference<T>, ? extends T> l, Function<? super HolderSet.Named<T>, ? extends T> r, Operation<String> original, @Share("special_predicate") LocalRef<EnhancedEntryPredicate.AnyOf<T>> anyOfShare, @Local(argsOnly = true) Pair<BlockPos, ? extends Holder<?>> result) {
    final EnhancedEntryPredicate.AnyOf<T> get = anyOfShare.get();
    if (get != null) {
      return get.asPrintable() + " (" + result.getSecond().getRegisteredName() + ")";
    } else {
      return original.call(instance, l, r);
    }
  }
}
