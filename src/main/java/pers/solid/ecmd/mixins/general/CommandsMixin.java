package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.util.extension.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.mixin.MixinShared;

@Mixin(Commands.class)
public abstract class CommandsMixin {
  @Inject(method = "finishParsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/String;substring(I)Ljava/lang/String;"), to = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle([Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;")), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void injectedAppendText(ParseResults<CommandSourceStack> parseResults, String command, CommandSourceStack source, CallbackInfoReturnable<ContextChain<CommandSourceStack>> cir, CommandSyntaxException commandSyntaxException, int i, MutableComponent mutableText) {
    final int cursorEnd = ((CommandSyntaxExceptionExtension) commandSyntaxException).getCursorEnd$ec();
    if (cursorEnd >= i) {
      mutableText.append(Component.literal("»").withStyle(ChatFormatting.DARK_RED));
    }
  }

  @ModifyArg(method = "finishParsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/String;substring(I)Ljava/lang/String;"), to = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle([Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;")))
  private static String modifiedGetErrorMessage(String string, @Local CommandSyntaxException commandSyntaxException, @Local int i) {
    if (commandSyntaxException != null) {
      final int cursorEnd = Math.min(commandSyntaxException.getInput().length(), ((CommandSyntaxExceptionExtension) commandSyntaxException).getCursorEnd$ec());
      if (cursorEnd >= i) {
        return string.substring(0, cursorEnd - i);
      }
    }
    return string;
  }

  @Inject(method = "finishParsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;append(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;", shift = At.Shift.AFTER), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"), to = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;")), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void injectedAppendText(ParseResults<CommandSourceStack> parseResults, String command, CommandSourceStack source, CallbackInfoReturnable<ContextChain<CommandSourceStack>> cir, CommandSyntaxException commandSyntaxException, int i, MutableComponent mutableText, Component text) {
    final int cursorEnd = ((CommandSyntaxExceptionExtension) commandSyntaxException).getCursorEnd$ec();
    if (cursorEnd >= i) {
      mutableText.append(Component.literal("«").withStyle(ChatFormatting.DARK_RED));
      mutableText.append(Component.literal(commandSyntaxException.getInput().substring(cursorEnd, Math.min(cursorEnd + 10, commandSyntaxException.getInput().length()))));
    }
  }

  @ModifyExpressionValue(method = "performCommand", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;isDebugEnabled()Z", remap = false))
  public boolean forceEnableDebugging(boolean original) {
    return true;
  }

  @Inject(method = "<init>", at = @At("TAIL"))
  private void storeCommandBuildContext(Commands.CommandSelection environment, CommandBuildContext commandBuildContext, CallbackInfo ci) {
    MixinShared.setWeakCommandBuildContext(commandBuildContext);
  }
}
