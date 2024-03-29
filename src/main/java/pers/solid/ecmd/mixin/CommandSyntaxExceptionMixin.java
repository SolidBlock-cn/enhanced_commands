package pers.solid.ecmd.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

@Mixin(value = CommandSyntaxException.class, remap = false)
public abstract class CommandSyntaxExceptionMixin implements CommandSyntaxExceptionExtension {
  @Shadow
  @Final
  private String input;
  @Unique
  private int cursorEnd = -1;

  @Override
  public int ec$getCursorEnd() {
    return cursorEnd;
  }

  @Override
  public void ec$setCursorEnd(int cursorEnd) {
    this.cursorEnd = cursorEnd;
  }

  @Inject(method = "getContext", at = @At(value = "INVOKE", target = "Ljava/lang/StringBuilder;append(Ljava/lang/String;)Ljava/lang/StringBuilder;", shift = At.Shift.AFTER), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/String;substring(II)Ljava/lang/String;", shift = At.Shift.AFTER), to = @At(value = "CONSTANT", args = "stringValue=<--[HERE]")), locals = LocalCapture.CAPTURE_FAILSOFT)
  public void injectedGetContext(CallbackInfoReturnable<String> cir, StringBuilder builder, int cursor) {
    final int cursorEnd = Math.min(ec$getCursorEnd(), input.length());
    if (cursorEnd >= 0 && cursorEnd > cursor) {
      builder.append('»');
      builder.append(input, cursor, cursorEnd);
      builder.append('«');
    }
  }
}
