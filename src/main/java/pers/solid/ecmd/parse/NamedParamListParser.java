package pers.solid.ecmd.parse;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.ModCommandExceptionTypes;

import java.util.Collection;

/**
 * 用于解析形如这种形式的命名参数：
 * <pre>key1 = value1, key2 = value2, key3 = value3</pre>
 * <p>
 * 解析过程中通常只是进行一次操作，或者修改一次字段，并不会实际返回值，因此该接口并不实现 {@link Parser}。
 */
public interface NamedParamListParser {

  /**
   * 支持的参数名称的集合。通常是一个不可变的集合，一般来说是 {@code Set}。
   */
  @Contract(pure = true)
  @Unmodifiable
  Collection<String> supportedParams();

  /**
   * 判断参数是否为有效参数，例如检查参数是否重复。不会抛出错误。该方法通常用于命令建议。返回值为 {@code false} 的参数不会被建议。
   *
   * @implNote 通常不需要判断 {@link #supportedParams()} 是否包含 {@code paramName}。
   */
  default boolean isValidParamName(String paramName) {
    return !isDuplicateParamName(paramName);
  }

  /**
   * 判断参数是否为重复参数。
   *
   * @param paramName 参数名称。
   * @return 该参数是否重复。如果重复，返回 {@code false}。
   */
  boolean isDuplicateParamName(String paramName);

  /**
   * 检查参数名称是否有效，如果无效，则抛出错误。默认会检查参数是否受支持，但不会检查重复的参数。
   *
   * @param paramName    参数名称。
   * @param stringReader 解析时使用的 StringReader 对象，可用于创建 {@link CommandSyntaxException}。
   * @param cursorBefore 参数名称前的位点。
   * @param cursorAfter  参数名称后的位点。
   */
  default void checkParamNameValidity(String paramName, StringReader stringReader, int cursorBefore, int cursorAfter) throws CommandSyntaxException {
    if (!supportedParams().contains(paramName)) {
      stringReader.setCursor(cursorBefore);
      throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.UNKNOWN_KEYWORD.createWithContext(stringReader, paramName), cursorAfter);
    }
    if (isDuplicateParamName(paramName)) {
      stringReader.setCursor(cursorBefore);
      throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(stringReader, paramName), cursorAfter);
    }
  }

  default char separatorChar() {
    return ',';
  }

  default char keyValueSeparatorChar() {
    return '=';
  }

  default char terminateChar() {
    return ')';
  }

  /**
   * 解析一个参数，此时 {@code cursor} 已经在等号后面，并验证了参数名称，且跳过了空白字符。
   *
   * @param paramName 参数名称。
   */
  void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException;

  default void parseNamedParameters(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    reader.skipWhitespace();
    int paramCount = 0;
    if (!reader.canRead() || reader.peek() != terminateChar()) while (true) {
      final char KEY_VALUE_SEP = keyValueSeparatorChar();
      final char PARAMS_SEP = separatorChar();
      parseContext.addSuggestion((commandContext, suggestionsBuilder) -> SharedSuggestionProvider.suggest(supportedParams().stream().filter(this::isValidParamName).map(s -> s + KEY_VALUE_SEP), suggestionsBuilder));
      final int cursorBeforeParamName = reader.getCursor();
      final String paramName = reader.readUnquotedString();
      final int cursorAfterParamName = reader.getCursor();

      paramCount++;

      checkParamNameValidity(paramName, reader, cursorBeforeParamName, cursorAfterParamName);

      reader.skipWhitespace();

      parseContext.setSuggestion((commandContext, suggestionsBuilder) -> suggestionsBuilder.suggest(Character.toString(KEY_VALUE_SEP)).buildFuture());

      reader.expect(KEY_VALUE_SEP);
      reader.skipWhitespace();

      parseContext.clearSuggestion();
      parseNamedParameter(paramName, parseContext);

      reader.skipWhitespace();

      // 如果还有可用的参数名称，提供逗号的建议
      if (supportedParams().stream().anyMatch(this::isValidParamName)) {
        parseContext.setSuggestion((commandContext, suggestionsBuilder) -> suggestionsBuilder.suggest(Character.toString(PARAMS_SEP)).buildFuture());
      } else {
        parseContext.clearSuggestion();
      }
      if (reader.canRead() && reader.peek() == PARAMS_SEP) {
        reader.skip();
        reader.skipWhitespace();
        parseContext.clearSuggestion();

        if (reader.canRead() && reader.peek() == terminateChar()) {
          return;
        }
      } else {
        break;
      }
    }
    else {
      return;
    }

  }
}
