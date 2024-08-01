package pers.solid.ecmd.mixins.impl;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;

@Mixin(value = CommandSyntaxException.class, remap = false)
public abstract class CommandSyntaxExceptionExtensionImpl implements CommandSyntaxExceptionExtension {
  @Unique
  private int cursorEnd = -1;

  @Override
  public int getCursorEnd$ec() {
    return cursorEnd;
  }

  @Override
  public void setCursorEnd$ec(int cursorEnd) {
    this.cursorEnd = cursorEnd;
  }
}
