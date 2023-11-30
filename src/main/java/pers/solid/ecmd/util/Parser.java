package pers.solid.ecmd.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import pers.solid.ecmd.argument.SuggestedParser;

/**
 * 此接口指定了特定环境下的文本的解析方法，在实现的抽象方法 {@link #parse} 中指定如何进行解析。解析过程中会移动在被解析的字符串中读取的位置，同时通常还会指定如何提供建议。遇到无效语法时，可能会抛出 {@link CommandSyntaxException}。
 *
 * @param <T> 被解析的对象的类型。
 * @see com.mojang.brigadier.arguments.ArgumentType#parse(StringReader)
 * @see SuggestedParser
 * @see StringReader
 */
@FunctionalInterface
public interface Parser<T> {
  /**
   * 在特定的环境下朝廷解析。
   *
   * @param commandRegistryAccess 此对象常用于命令中，用于从注册表中获取一些信息，常见于方块、物品、实体等的 ID 解析过程中。
   * @param parser                包含 {@link StringReader} 和 {@link SuggestionProvider} 的对象。解析过程中，可以移动其 {@code cursor}，并指定如何提供建议。
   * @param suggestionsOnly       解析过程中是否为提供建议。如果为 {@code true}，那么一些不影响后续解析过程的操作可以不进行。
   * @param allowSparse           对于特定类型的语法，是否允许各部分用空格隔开。一般来说，直接用作命令参数、外面没有括号时，是 {@code false}。如果是在括号（或有明显其他割开定界符的环境）内解析，则为 {@code true}。
   * @return 解析后的结果，
   * @throws CommandSyntaxException 解析时遇到的语法错误。
   */
  T parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowSparse) throws CommandSyntaxException;
}
