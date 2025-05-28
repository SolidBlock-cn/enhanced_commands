package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.StringReader;
import net.minecraft.command.argument.NbtPathArgumentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(NbtPathArgumentType.class)
public abstract class NbtPathArgumentTypeMixin {
  /**
   * 在读取 Nbt Path 时，除了读到空格时中止外，还应在读到其他一些特殊的符号字符时中止，以避免无法正常在函数式语法中解析。
   */
  @ModifyExpressionValue(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/command/argument/NbtPathArgumentType$NbtPath;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;canRead()Z"), slice = @Slice(to = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;peek()C")))
  private boolean stopReadingMoreChars(boolean original, @Local(argsOnly = true) StringReader reader) {
    if (original) {
      final char peek = reader.peek();
      if (peek == ',' || peek == ';' || peek == ')' || peek == ']' || peek == '}') {
        return false;
      }
    }

    return original;
  }
}
