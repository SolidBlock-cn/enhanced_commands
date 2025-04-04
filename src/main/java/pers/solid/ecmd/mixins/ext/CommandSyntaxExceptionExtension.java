package pers.solid.ecmd.mixins.ext;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Contract;

/**
 * <p>此类将被注入至 {@link CommandSyntaxException}，以实现更多的方法。但由于目标类属于 Brigadier，所以无法在编译时注入。
 * <p>此注入的目的是在 {@code CommandSyntaxException} 中加入新的字段 {@code cursorEnd}，表示指针的结束位置。在原版中，每个异常有一个指针位置，表示字符读取过程中出错的位置，例如：
 * <pre>red green <u>[cursor]</u>water blue yellow</pre>
 * <p>在显示错误时，上述内容会显示成这样：
 * <pre>red green <u>water blue yellow</u><span><-[此处]</span></pre>
 * <p>这种写法只会显示是从哪里开始出错的，但并没有显示具体是哪一部分的内容出错。因此，本模组引入了 {@code cursorEnd}，表示字符串中出错的结束部分。例如：
 * <pre>red green <u>[cursor]</u>water<u>[cursorEnd]</u> blue yellow</pre>
 * <p>这样，显示错误时，就能够精准地显示出是哪一部分出现的错误：
 * <pre>red green <span style="color:maroon">»</span><u>water</u><span style="color:maroon">«</span> blue yellow<span><-[此处]</span></pre>
 *
 * @see pers.solid.ecmd.mixins.impl.CommandSyntaxExceptionExtensionImpl
 */
public interface CommandSyntaxExceptionExtension {
  @Contract(pure = true)
  default int getCursorEnd$ec() {
    throw new UnsupportedOperationException();
  }

  default void setCursorEnd$ec(int cursorEnd) {
    throw new UnsupportedOperationException();
  }

  /**
   * 设置 {@link CommandSyntaxException} 的 {@code cursorEnd}，并返回它。这种静态调用可以省略掉类型转换的过程。
   */
  static <T extends CommandSyntaxException> T withCursorEnd(T exception, int cursorEnd) {
    ((CommandSyntaxExceptionExtension) exception).setCursorEnd$ec(cursorEnd);
    return exception;
  }

  /**
   * 设置 {@link CommandSyntaxException} 的 {@code cursorEnd} 为 {@code cursor + length}。
   */
  static <T extends CommandSyntaxException> T addCursorEnd(T exception, int length) {
    return withCursorEnd(exception, exception.getCursor() + length);
  }

  /**
   * 设置 {@link CommandSyntaxException} 的 {@code cursorEnd} 为 {@code cursor + refString.length()}，也就是根据字符串 {@code refString} 的长度来决定指针的结束位置。
   */
  static <T extends CommandSyntaxException> T addCursorEnd(T exception, String refString) {
    return addCursorEnd(exception, refString.length());
  }
}
