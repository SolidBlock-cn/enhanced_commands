package pers.solid.ecmd.mixin;

import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Identifier.class)
public abstract class IdentifierMixin {/*
// todo complete here
  @ModifyExpressionValue(method = "fromCommandInput", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException modifiedException(CommandSyntaxException commandSyntaxException, @Local int cursor, @Local String string) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, cursor + string.length());
  }*/
}
