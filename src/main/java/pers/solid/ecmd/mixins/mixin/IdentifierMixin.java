package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.ModCommandExceptionTypes;

@Mixin(Identifier.class)
public abstract class IdentifierMixin {
  @Unique
  private static boolean isUpperCase(char c) {
    return c >= 'A' && c <= 'Z';
  }

  @ModifyExpressionValue(method = "readString", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Identifier;isCharValid(C)Z"))
  private static boolean recognizeMoreChars(boolean original, @Local(argsOnly = true) StringReader reader) {
    return original || isUpperCase(reader.peek());
  }

  @Inject(method = {"fromCommandInput", "fromCommandInputNonEmpty"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Identifier;readString(Lcom/mojang/brigadier/StringReader;)Ljava/lang/String;", shift = At.Shift.AFTER))
  private static void storeCursor(StringReader reader, CallbackInfoReturnable<Identifier> cir, @Share("cursorAfterString") LocalIntRef cursorAfterString) {
    cursorAfterString.set(reader.getCursor());
  }

  @ModifyExpressionValue(method = {"fromCommandInput", "fromCommandInputNonEmpty"}, at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException tweakCommandException(CommandSyntaxException commandSyntaxException, @Share("cursorAfterString") LocalIntRef cursorAfterString, @Local(argsOnly = true) StringReader reader) {
    final String input = reader.getString();
    for (int i = reader.getCursor(); i < cursorAfterString.get(); i++) {
      final char c = input.charAt(i);
      if (isUpperCase(c)) {
        return CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.CONTAINS_UPPER_CASE.createWithContext(reader), cursorAfterString.get());
      }
    }
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, cursorAfterString.get());
  }
}
