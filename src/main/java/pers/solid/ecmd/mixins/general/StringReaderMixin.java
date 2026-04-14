package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;

@Mixin(value = StringReader.class, remap = false)
public abstract class StringReaderMixin {
  @Shadow
  private int cursor;

  @Shadow
  public abstract String readString();

  @Shadow
  public abstract String readUnquotedString();

  /**
   * 当没有解析到内容（即抛出需要某个值的错误）时，尝试读取一下后面的内容并设置其 cursorEnd
   */
  @Unique
  private CommandSyntaxException enhanced_commands$trySetCursorEndForNone(CommandSyntaxException commandSyntaxException) {
    final int initialCursor = cursor;
    readUnquotedString();
    final int cursorAfterUnquotedString = cursor;
    if (cursorAfterUnquotedString > initialCursor) {
      commandSyntaxException = EnhancedCommandSyntaxException.withCursorEnd(commandSyntaxException, cursorAfterUnquotedString);
      cursor = initialCursor;
    }
    return commandSyntaxException;
  }

  @ModifyExpressionValue(remap = false, method = "readInt", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(to = @At(value = "INVOKE", target = "Ljava/lang/Integer;parseInt(Ljava/lang/String;)I")))
  public CommandSyntaxException modifiedReadNoneInt(CommandSyntaxException commandSyntaxException) {
    return enhanced_commands$trySetCursorEndForNone(commandSyntaxException);
  }

  @ModifyExpressionValue(remap = false, method = "readInt", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/Integer;parseInt(Ljava/lang/String;)I")))
  public CommandSyntaxException modifiedReadInvalidInt(CommandSyntaxException commandSyntaxException, @Local(name = "number") String number) {
    return EnhancedCommandSyntaxException.addCursorEnd(commandSyntaxException, number);
  }

  @ModifyExpressionValue(remap = false, method = "readLong", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(to = @At(value = "INVOKE", target = "Ljava/lang/Long;parseLong(Ljava/lang/String;)J")))
  public CommandSyntaxException modifiedReadNoneLong(CommandSyntaxException commandSyntaxException) {
    return enhanced_commands$trySetCursorEndForNone(commandSyntaxException);
  }

  @ModifyExpressionValue(remap = false, method = "readLong", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/Long;parseLong(Ljava/lang/String;)J")))
  public CommandSyntaxException modifiedReadInvalidLong(CommandSyntaxException commandSyntaxException, @Local(name = "number") String number) {
    return EnhancedCommandSyntaxException.addCursorEnd(commandSyntaxException, number);
  }


  @ModifyExpressionValue(remap = false, method = "readDouble", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(to = @At(value = "INVOKE", target = "Ljava/lang/Double;parseDouble(Ljava/lang/String;)D")))
  public CommandSyntaxException modifiedReadNoneDouble(CommandSyntaxException commandSyntaxException) {
    return enhanced_commands$trySetCursorEndForNone(commandSyntaxException);
  }

  @ModifyExpressionValue(remap = false, method = "readDouble", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/Double;parseDouble(Ljava/lang/String;)D")))
  public CommandSyntaxException modifiedReadInvalidDouble(CommandSyntaxException commandSyntaxException, @Local(name = "number") String number) {
    return EnhancedCommandSyntaxException.addCursorEnd(commandSyntaxException, number);
  }

  @ModifyExpressionValue(remap = false, method = "readFloat", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(to = @At(value = "INVOKE", target = "Ljava/lang/Float;parseFloat(Ljava/lang/String;)F")))
  public CommandSyntaxException modifiedReadNoneFloat(CommandSyntaxException commandSyntaxException) {
    return enhanced_commands$trySetCursorEndForNone(commandSyntaxException);
  }

  @ModifyExpressionValue(remap = false, method = "readFloat", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/Float;parseFloat(Ljava/lang/String;)F")))
  public CommandSyntaxException modifiedReadInvalidFloat(CommandSyntaxException commandSyntaxException, @Local(name = "number") String number) {
    return EnhancedCommandSyntaxException.addCursorEnd(commandSyntaxException, number);
  }

  @ModifyExpressionValue(remap = false, method = "readStringUntil", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(from = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/BuiltInExceptionProvider;readerInvalidEscape()Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;")))
  public CommandSyntaxException modifiedReadStringUntil(CommandSyntaxException commandSyntaxException) {
    return EnhancedCommandSyntaxException.addCursorEnd(commandSyntaxException, 1);
  }

  @ModifyExpressionValue(remap = false, method = "readBoolean", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;"), slice = @Slice(from = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/BuiltInExceptionProvider;readerInvalidBool()Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;")))
  public CommandSyntaxException modifiedReadBoolean(CommandSyntaxException commandSyntaxException, @Local(name = "start") int start, @Local(name = "value") String value) {
    return EnhancedCommandSyntaxException.withCursorEnd(commandSyntaxException, start + value.length());
  }
}
