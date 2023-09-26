package pers.solid.ecmd.util.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

public interface CommandSyntaxExceptionExtension {
  void ec$setCursorEnd(int cursorEnd);

  int ec$getCursorEnd();

  static CommandSyntaxException withCursorEnd(CommandSyntaxException exception, int cursorEnd) {
    ((CommandSyntaxExceptionExtension) exception).ec$setCursorEnd(cursorEnd);
    return exception;
  }
}
