package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.util.StringIdentifiable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.util.function.Supplier;

@Mixin(EnumArgumentType.class)
public abstract class EnumArgumentTypeMixin<T extends Enum<T> & StringIdentifiable> {
  @Shadow
  @Final
  private static DynamicCommandExceptionType INVALID_ENUM_EXCEPTION;

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Ljava/lang/Enum;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", shift = At.Shift.BEFORE, remap = false))
  public void injectedParse(StringReader stringReader, CallbackInfoReturnable<T> cir, @Share("cursorBeforeUnquotedString") LocalIntRef localIntRef) {
    localIntRef.set(stringReader.getCursor());
  }

  @ModifyArg(method = "parse(Lcom/mojang/brigadier/StringReader;)Ljava/lang/Enum;", at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseThrow(Ljava/util/function/Supplier;)Ljava/lang/Object;"))
  public Supplier<CommandSyntaxException> modifiedParse(Supplier<CommandSyntaxException> exceptionSupplier, @Local StringReader stringReader, @Local String string, @Share("cursorBeforeUnquotedString") LocalIntRef localIntRef) {
    return () -> {
      final int cursorAfterUnquotedString = stringReader.getCursor();
      final int cursorBeforeUnquotedString = localIntRef.get();
      stringReader.setCursor(cursorBeforeUnquotedString);
      return CommandSyntaxExceptionExtension.withCursorEnd(INVALID_ENUM_EXCEPTION.createWithContext(stringReader, string), cursorAfterUnquotedString);
    };
  }
}
