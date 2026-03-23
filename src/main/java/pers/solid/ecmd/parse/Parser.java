package pers.solid.ecmd.parse;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Nullable;

/**
 * 此接口指定了特定环境下的文本的解析方法，在实现的抽象方法 {@link #parse} 中指定如何进行解析。解析过程中会移动在被解析的字符串中读取的位置，同时通常还会指定如何提供建议。遇到无效语法时，可能会抛出 {@link CommandSyntaxException}。
 *
 * @param <T> 被解析的对象的类型。
 * @see com.mojang.brigadier.arguments.ArgumentType#parse(StringReader)
 * @see StringReader
 */
@FunctionalInterface
public interface Parser<T> {
  /**
   * 在特定的环境下朝廷解析。
   *
   * @return 解析后的结果，
   * @throws CommandSyntaxException 解析时遇到的语法错误。
   */
  @Nullable T parse(ParseContext<?> parseContext) throws CommandSyntaxException;
}
