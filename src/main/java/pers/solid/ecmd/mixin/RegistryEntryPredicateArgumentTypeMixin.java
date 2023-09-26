package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.RegistryEntryPredicateArgumentType;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.util.function.Supplier;

@Mixin(RegistryEntryPredicateArgumentType.class)
public abstract class RegistryEntryPredicateArgumentTypeMixin<T> {
  @Inject(
      method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryPredicate;",
      at = @At("HEAD")
  )
  public void injectedParseHead(StringReader stringReader, CallbackInfoReturnable<RegistryEntry.Reference<T>> cir, @Share("cursorBeforeId") LocalIntRef localIntRef) {
    localIntRef.set(stringReader.getCursor());
  }

  @ModifyArg(
      method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryPredicate;",
      at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseThrow(Ljava/util/function/Supplier;)Ljava/lang/Object;"),
      slice = @Slice(
          from = @At(value = "INVOKE", target = "Lnet/minecraft/registry/tag/TagKey;of(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/util/Identifier;)Lnet/minecraft/registry/tag/TagKey;"),
          to = @At(value = "INVOKE", target = "Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$TagBased;<init>(Lnet/minecraft/registry/entry/RegistryEntryList$Named;)V")
      )
  )
  public Supplier<CommandSyntaxException> modifiedParseTagException(Supplier<CommandSyntaxException> exceptionSupplier, @Share("cursorBeforeId") LocalIntRef localIntRef, @Local StringReader stringReader) {
    return () -> {
      final int cursorBeforeId = localIntRef.get();
      final int cursorAfterId = stringReader.getCursor();
      stringReader.setCursor(cursorBeforeId);
      final CommandSyntaxException commandSyntaxException = exceptionSupplier.get();
      return CommandSyntaxExceptionExtension.withCursorEnd(new CommandSyntaxException(commandSyntaxException.getType(), commandSyntaxException.getRawMessage(), stringReader.getString(), stringReader.getCursor()), cursorAfterId);
    };
  }

  @ModifyArg(
      method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryPredicate;",
      at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseThrow(Ljava/util/function/Supplier;)Ljava/lang/Object;"),
      slice = @Slice(
          from = @At(value = "INVOKE", target = "Lnet/minecraft/registry/RegistryKey;of(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/util/Identifier;)Lnet/minecraft/registry/RegistryKey;"),
          to = @At(value = "INVOKE", target = "Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryBased;<init>(Lnet/minecraft/registry/entry/RegistryEntry$Reference;)V")
      )
  )
  public Supplier<CommandSyntaxException> modifiedParseEntryException(Supplier<CommandSyntaxException> exceptionSupplier, @Share("cursorBeforeId") LocalIntRef localIntRef, @Local StringReader stringReader) {
    return () -> {
      final int cursorBeforeId = localIntRef.get();
      final int cursorAfterId = stringReader.getCursor();
      stringReader.setCursor(cursorBeforeId);
      final CommandSyntaxException commandSyntaxException = exceptionSupplier.get();
      return CommandSyntaxExceptionExtension.withCursorEnd(new CommandSyntaxException(commandSyntaxException.getType(), commandSyntaxException.getRawMessage(), stringReader.getString(), stringReader.getCursor()), cursorAfterId);
    };
  }
}
