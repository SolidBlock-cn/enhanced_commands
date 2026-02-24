package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.NbtPathArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import pers.solid.ecmd.config.GeneralParsingConfig;

@Mixin(NbtPathArgument.class)
public abstract class NbtPathArgumentMixin {
  /**
   * 在读取 Nbt Path 时，除了读到空格时中止外，还应在读到其他一些特殊的符号字符时中止，以避免无法正常在函数式语法中解析。
   */
  @ModifyExpressionValue(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/NbtPathArgument$NbtPath;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;canRead()Z", remap = false), slice = @Slice(to = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;peek()C", remap = false)))
  private boolean stopReadingMoreChars(boolean original, @Local(argsOnly = true) StringReader reader) {
    if (original && GeneralParsingConfig.current.improvedNbtPathParsing) {
      final char peek = reader.peek();
      if (peek == ',' || peek == ';' || peek == ')' || peek == ']' || peek == '}') {
        return false;
      }
    }

    return original;
  }
}
