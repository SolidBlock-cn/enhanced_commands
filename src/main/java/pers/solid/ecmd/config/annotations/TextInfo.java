package pers.solid.ecmd.config.annotations;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于以注解的方式表示文本。仅用于注解的值中，不可注解于任何内容。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface TextInfo {
  /**
   * 文本的内容，包括其基本格式信息，但是不包括翻译参数和附加内容。
   */
  TextEntry value() default @TextEntry;

  /**
   * 文本的翻译参数，仅适用于 {@link TextEntry#type()} 为 {@link TextEntry.Type#TRANSLATABLE} 的情况。但是，如果 {@link TextEntry#value()} 为默认字符串导致文本为使用默认翻译键的可翻译文本（包括此注解的 {@link #value()} 完全未指定的情况），则仍会使用此参数。
   */
  TextEntry[] args() default {};

  /**
   * 在文本后附加的文本，相当于 {@link MutableComponent#append(Component)}。
   */
  TextEntry[] append() default {};
}
