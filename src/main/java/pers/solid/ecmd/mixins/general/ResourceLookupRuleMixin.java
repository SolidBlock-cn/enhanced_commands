package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.commands.ResourceLookupRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;

@Mixin(ResourceLookupRule.class)
public class ResourceLookupRuleMixin {

  @ModifyArg(method = "parse", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/parsing/packrat/ErrorCollector;store(ILnet/minecraft/util/parsing/packrat/SuggestionSupplier;Ljava/lang/Object;)V"), index = 2)
  private Object modifyRecordedException(Object reason, @Local int markBeforeParsing, @Local(argsOnly = true) ParseState<StringReader> parseState) {
    // 若为 CommandSyntaxException，处理其 cursor 参数，使其能正确指向错误位置。

    if (reason instanceof CommandSyntaxException e) {
      return new EnhancedCommandSyntaxException(e.getType(), e.getRawMessage(), e.getInput(), e.getCursor() == parseState.mark() ? markBeforeParsing : e.getCursor(), e instanceof EnhancedCommandSyntaxException en ? en.getCursorEnd() : e.getCursor());
    } else {
      return reason;
    }
  }
}
