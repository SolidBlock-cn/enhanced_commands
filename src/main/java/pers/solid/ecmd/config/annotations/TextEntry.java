package pers.solid.ecmd.config.annotations;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 以注解的方式表示一个文本的内容。此注解用作 {@link TextInfo} 的各参数，可指定文本的格式等信息。
 *
 * @see TextInfo
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface TextEntry {
  String EMPTY_STRING_VALUE = "";

  /**
   * 文本组件的类型，目前只支持 {@link Type} 中的一些值，需要指定其他值请使用非注解的方式修改。当 {@link #value()} 的值为默认值时，此方法会被忽略，并将使用自动生成的翻译键为翻译键的可翻译文本组件。
   */
  Type type() default Type.LITERAL;

  /**
   * 文本组件的值，例如字面字符串的内容或可翻译文本组件的键。当此字段使用默认值时，将表示使用可翻译文本组件，其翻译键将自动生成。
   */
  String value() default EMPTY_STRING_VALUE;

  /**
   * 文本的格式。
   *
   * @see MutableComponent#withStyle(ChatFormatting)
   */
  ChatFormatting[] formatting() default {};

  /**
   * 自定义文本的颜色。考虑到多版本的兼容性，建议使用带有 alpha 的颜色，例如 {@code 0xffeeaa00} 而非 {@code 0xeeaa00}。
   *
   * @see MutableComponent#withColor(int)
   */
  int textColor() default 0;

  enum Type {
    /**
     * @see Component#literal(String)
     */
    LITERAL,
    /**
     * @see Component#translatable(String)
     */
    TRANSLATABLE,
    /**
     * @see Component#keybind(String)
     */
    KEYBIND
  }
}
