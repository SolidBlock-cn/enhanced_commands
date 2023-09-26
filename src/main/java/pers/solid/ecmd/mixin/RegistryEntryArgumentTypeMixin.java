package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.util.function.Supplier;

@Mixin(RegistryEntryArgumentType.class)
public abstract class RegistryEntryArgumentTypeMixin<T> {
  @Shadow
  @Final
  public static Dynamic2CommandExceptionType NOT_FOUND_EXCEPTION;

  @Shadow
  @Final
  RegistryKey<? extends Registry<T>> registryRef;

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/registry/entry/RegistryEntry$Reference;", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Identifier;fromCommandInput(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/util/Identifier;", shift = At.Shift.BEFORE))
  public void injectedParse(StringReader stringReader, CallbackInfoReturnable<RegistryEntry.Reference<T>> cir, @Share("cursorBeforeId") LocalIntRef localIntRef) {
    localIntRef.set(stringReader.getCursor());
  }

  @ModifyArg(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/registry/entry/RegistryEntry$Reference;", at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseThrow(Ljava/util/function/Supplier;)Ljava/lang/Object;"))
  public Supplier<CommandSyntaxException> modifiedParseThrow(Supplier<CommandSyntaxException> exceptionSupplier, @Share("cursorBeforeId") LocalIntRef localIntRef, @Local StringReader stringReader, @Local Identifier identifier) {
    return () -> {
      final int cursorAfterId = stringReader.getCursor();
      final int cursorBeforeId = localIntRef.get();
      stringReader.setCursor(cursorBeforeId);
      return CommandSyntaxExceptionExtension.withCursorEnd(NOT_FOUND_EXCEPTION.createWithContext(stringReader, identifier, registryRef.getValue()), cursorAfterId);
    };
  }
}
