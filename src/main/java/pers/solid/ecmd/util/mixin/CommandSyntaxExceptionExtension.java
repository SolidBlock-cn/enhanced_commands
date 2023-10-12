package pers.solid.ecmd.util.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Contract;

public interface CommandSyntaxExceptionExtension {
  @Contract(mutates = "this")
  void ec$setCursorEnd(int cursorEnd);

  @Contract(pure = true)
  int ec$getCursorEnd();

  @Contract(value = "_, _ -> param1", mutates = "param1")
  static <T extends CommandSyntaxException> T withCursorEnd(T exception, int cursorEnd) {
    ((CommandSyntaxExceptionExtension) exception).ec$setCursorEnd(cursorEnd);
    return exception;
  }
}
