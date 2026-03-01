package pers.solid.ecmd.util;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * <p>此类为增强的 {@link CommandSyntaxException}，以实现更多的方法。相比，{@code CommandSyntaxException}，此类加入加入新的字段 {@link #cursorEnd}，表示指针的结束位置。在原版中，每个异常有一个指针位置，表示字符读取过程中出错的位置，例如：
 * <pre>red green <u>[cursor]</u>water blue yellow</pre>
 * <p>在显示错误时，上述内容会显示成这样：
 * <pre>red green <u>water blue yellow</u><span><-[此处]</span></pre>
 * <p>这种写法只会显示是从哪里开始出错的，但并没有显示具体是哪一部分的内容出错。因此，本模组引入了 {@code cursorEnd}，表示字符串中出错的结束部分。例如：
 * <pre>red green <u>[cursor]</u>water<u>[cursorEnd]</u> blue yellow</pre>
 * <p>这样，显示错误时，就能够精准地显示出是哪一部分出现的错误：
 * <pre>red green <span style="color:maroon">»</span><u>water</u><span style="color:maroon">«</span> blue yellow<span><-[此处]</span></pre>
 */
public class EnhancedCommandSyntaxException extends CommandSyntaxException {
  private final int cursorEnd;

  public EnhancedCommandSyntaxException(CommandExceptionType type, Message message, String input, int cursor, int cursorEnd) {
    super(type, message, input, cursor);
    this.cursorEnd = cursorEnd;
  }

  /**
   * 设置 {@link CommandSyntaxException} 的 {@code cursorEnd}，并返回它。这种静态调用可以省略掉类型转换的过程。
   */
  public static EnhancedCommandSyntaxException withCursorEnd(CommandSyntaxException exception, int cursorEnd) {
    return new EnhancedCommandSyntaxException(exception.getType(), exception.getRawMessage(), exception.getInput(), exception.getCursor(), cursorEnd);
  }

  /**
   * 设置 {@link CommandSyntaxException} 的 {@code cursorEnd} 为 {@code cursor + length}。
   */
  public static EnhancedCommandSyntaxException addCursorEnd(CommandSyntaxException exception, int length) {
    return withCursorEnd(exception, exception.getCursor() + length);
  }

  /**
   * 设置 {@link CommandSyntaxException} 的 {@code cursorEnd} 为 {@code cursor + refString.length()}，也就是根据字符串 {@code refString} 的长度来决定指针的结束位置。
   */
  public static EnhancedCommandSyntaxException addCursorEnd(CommandSyntaxException exception, String refString) {
    return addCursorEnd(exception, refString.length());
  }

  @Override
  public String getContext() {
    final String input = getInput();
    final int cursor = getCursor();
    if (input != null && cursor >= 0) {
      StringBuilder builder = new StringBuilder();
      int top = Math.min(input.length(), cursor);
      if (top > 10) {
        builder.append("...");
      }

      final int cursorEnd = Math.min(this.cursorEnd, input.length());
      if (cursorEnd >= 0 && cursorEnd > cursor) {
        builder.append('»');
        builder.append(input, cursor, cursorEnd);
        builder.append('«');
      }
      builder.append(input, Math.max(0, top - 10), top);
      builder.append("<--[HERE]");
      return builder.toString();
    } else {
      return null;
    }
  }

  public int getCursorEnd() {
    return cursorEnd;
  }

  public static int getCursorEndOf(CommandSyntaxException exception) {
    if (exception instanceof EnhancedCommandSyntaxException enhanced) {
      return enhanced.getCursorEnd();
    } else {
      return -1;
    }
  }
}
