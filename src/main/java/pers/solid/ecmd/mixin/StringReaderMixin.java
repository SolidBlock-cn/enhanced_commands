package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

@Mixin(StringReader.class)
public abstract class StringReaderMixin {
  @ModifyExpressionValue(remap = false, method = "readInt", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/Integer;parseInt(Ljava/lang/String;)I")))
  public CommandSyntaxException modifiedReadInt(CommandSyntaxException commandSyntaxException, @Local String number) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, commandSyntaxException.getCursor() + number.length());
  }

  @ModifyExpressionValue(remap = false, method = "readLong", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/Long;parseLong(Ljava/lang/String;)J")))
  public CommandSyntaxException modifiedReadLong(CommandSyntaxException commandSyntaxException, @Local String number) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, commandSyntaxException.getCursor() + number.length());
  }

  @ModifyExpressionValue(remap = false, method = "readDouble", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/Double;parseDouble(Ljava/lang/String;)D")))
  public CommandSyntaxException modifiedReadDouble(CommandSyntaxException commandSyntaxException, @Local String number) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, commandSyntaxException.getCursor() + number.length());
  }

  @ModifyExpressionValue(remap = false, method = "readFloat", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/Float;parseFloat(Ljava/lang/String;)F")))
  public CommandSyntaxException modifiedReadFloat(CommandSyntaxException commandSyntaxException, @Local String number) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, commandSyntaxException.getCursor() + number.length());
  }

  @ModifyExpressionValue(remap = false, method = "readStringUntil", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(from = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/BuiltInExceptionProvider;readerInvalidEscape()Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;")))
  public CommandSyntaxException modifiedReadStringUntil(CommandSyntaxException commandSyntaxException) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, commandSyntaxException.getCursor() + 1);
  }

  @ModifyExpressionValue(remap = false, method = "readBoolean", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(from = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/BuiltInExceptionProvider;readerInvalidBool()Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;")))
  public CommandSyntaxException modifiedReadBoolean(CommandSyntaxException commandSyntaxException, @Local int start, @Local String value) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, start + value.length());
  }
}
