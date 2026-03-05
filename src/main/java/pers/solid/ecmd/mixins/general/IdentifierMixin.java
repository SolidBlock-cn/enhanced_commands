package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.config.GeneralParsingConfig;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;

@Mixin(ResourceLocation.class)
public abstract class IdentifierMixin {
  @Unique
  private static boolean isUpperCase(char c) {
    return c >= 'A' && c <= 'Z';
  }

  @ModifyExpressionValue(method = "readGreedy", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;isAllowedInResourceLocation(C)Z"))
  private static boolean recognizeMoreChars(boolean original, @Local(argsOnly = true) StringReader reader) {
    return original || (GeneralParsingConfig.current.improvedIdParsing && isUpperCase(reader.peek()));
  }

  @Inject(method = {"read(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/resources/ResourceLocation;", "readNonEmpty"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;readGreedy(Lcom/mojang/brigadier/StringReader;)Ljava/lang/String;", shift = At.Shift.AFTER))
  private static void storeCursor(StringReader reader, CallbackInfoReturnable<ResourceLocation> cir, @Share("cursorAfterString") LocalIntRef cursorAfterString) {
    cursorAfterString.set(reader.getCursor());
  }

  @ModifyExpressionValue(method = {"read(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/resources/ResourceLocation;", "readNonEmpty"}, at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException tweakCommandException(CommandSyntaxException commandSyntaxException, @Share("cursorAfterString") LocalIntRef cursorAfterString, @Local(argsOnly = true) StringReader reader) {
    if (!GeneralParsingConfig.current.detailedIdentifierException) {
      // 当前配置未启用时，直接返回原版的。
      return commandSyntaxException;
    }
    final String input = reader.getString();
    for (int i = reader.getCursor(); i < cursorAfterString.get(); i++) {
      final char c = input.charAt(i);
      if (isUpperCase(c)) {
        return EnhancedCommandSyntaxException.withCursorEnd(EnhancedCommandsCommandExceptionTypes.CONTAINS_UPPER_CASE.createWithContext(reader), cursorAfterString.get());
      }
    }
    return EnhancedCommandSyntaxException.withCursorEnd(commandSyntaxException, cursorAfterString.get());
  }
}
