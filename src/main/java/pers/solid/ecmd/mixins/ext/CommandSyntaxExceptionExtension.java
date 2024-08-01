package pers.solid.ecmd.mixins.ext;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Contract;

/**
 * 此类将被注入至 {@link CommandSyntaxException}，以实现更多的方法。但由于目标类属于 Brigadier，所以无法在编译时注入。
 *
 * @see pers.solid.ecmd.mixins.impl.CommandSyntaxExceptionExtensionImpl
 */
public interface CommandSyntaxExceptionExtension {
  @Contract(mutates = "this")
  default void ec$setCursorEnd(int cursorEnd) {
    throw new UnsupportedOperationException();
  }

  @Contract(pure = true)
  default int ec$getCursorEnd() {
    throw new UnsupportedOperationException();
  }

  @Contract(value = "_, _ -> param1", mutates = "param1")
  static <T extends CommandSyntaxException> T withCursorEnd(T exception, int cursorEnd) {
    ((CommandSyntaxExceptionExtension) exception).ec$setCursorEnd(cursorEnd);
    return exception;
  }
}
