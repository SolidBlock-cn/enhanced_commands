package pers.solid.ecmd.mixins.impl;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.util.extension.CommandSyntaxExceptionExtension;

@Mixin(value = CommandSyntaxException.class, remap = false)
public abstract class CommandSyntaxExceptionExtensionImpl implements CommandSyntaxExceptionExtension {
  @Unique
  private int enhanced_commands$cursorEnd = -1;

  @Override
  public int getCursorEnd$ec() {
    return enhanced_commands$cursorEnd;
  }

  @Override
  public void setCursorEnd$ec(int cursorEnd) {
    this.enhanced_commands$cursorEnd = cursorEnd;
  }
}
