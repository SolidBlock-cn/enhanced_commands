package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

@Mixin(Identifier.class)
public abstract class IdentifierMixin {
  @ModifyExpressionValue(method = "fromCommandInput", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException modifiedException(CommandSyntaxException commandSyntaxException, @Local int cursor, @Local String string) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, cursor + string.length());
  }
}
