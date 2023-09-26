package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {
  @ModifyArg(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/Text;literal(Ljava/lang/String;)Lnet/minecraft/text/MutableText;"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/String;substring(I)Ljava/lang/String;"), to = @At(value = "INVOKE", target = "Lnet/minecraft/text/MutableText;formatted([Lnet/minecraft/util/Formatting;)Lnet/minecraft/text/MutableText;")))
  public String modifiedGetErrorMessage(String string, @Local CommandSyntaxException commandSyntaxException, @Local int i) {
    if (commandSyntaxException != null) {
      final int cursorEnd = Math.min(commandSyntaxException.getInput().length(), ((CommandSyntaxExceptionExtension) commandSyntaxException).ec$getCursorEnd());
      if (cursorEnd >= i) {
        return string.substring(0, cursorEnd - i);
      }
    }
    return string;
  }

  @Inject(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/MutableText;append(Lnet/minecraft/text/Text;)Lnet/minecraft/text/MutableText;", shift = At.Shift.AFTER), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/text/Text;literal(Ljava/lang/String;)Lnet/minecraft/text/MutableText;"), to = @At(value = "INVOKE", target = "Lnet/minecraft/text/Text;translatable(Ljava/lang/String;)Lnet/minecraft/text/MutableText;")), locals = LocalCapture.CAPTURE_FAILSOFT)
  public void injectedAppendText(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfoReturnable<Integer> cir, ServerCommandSource serverCommandSource, CommandSyntaxException commandSyntaxException, int i, MutableText mutableText, Text text) {
    final int cursorEnd = ((CommandSyntaxExceptionExtension) commandSyntaxException).ec$getCursorEnd();
    if (cursorEnd >= i) {
      mutableText.append(Text.literal(commandSyntaxException.getInput().substring(cursorEnd, Math.min(cursorEnd + 10, commandSyntaxException.getInput().length()))));
    }
  }
}
