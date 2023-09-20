package pers.solid.ecmd.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.chars.CharSet;
import net.minecraft.util.math.Position;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class StringUtil {
  private StringUtil() {
  }

  /**
   * @see StringReader#isAllowedInUnquotedString(char)
   */
  public static boolean isAllowedInUnquotedString(final @NotNull String s) {
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (!StringReader.isAllowedInUnquotedString(c))
        return false;
    }
    return true;
  }

  private static final CharSet EXTENDED_ALLOWED_STRINGS = CharSet.of('!', '@', '#', '$', '%', '^', '&', '*', '?', '\\');

  /**
   * 此方法可以接受更多的字符串，常用于正则表达式。但是，它仍然不接受括号。
   *
   * @see StringReader#isAllowedInUnquotedString(char)
   */
  public static boolean isAllowedInRegexUnquotedString(final char c) {
    return StringReader.isAllowedInUnquotedString(c) || EXTENDED_ALLOWED_STRINGS.contains(c);
  }

  /**
   * @see StringReader#readUnquotedString()
   */
  public static String readRegexUnquotedString(StringReader stringReader) {
    final int start = stringReader.getCursor();
    while (stringReader.canRead() && isAllowedInRegexUnquotedString(stringReader.peek())) {
      stringReader.skip();
    }
    return stringReader.getString().substring(start, stringReader.getCursor());
  }

  /**
   * @see StringReader#readString()
   */
  public static String readRegexString(StringReader stringReader) throws CommandSyntaxException {
    if (!stringReader.canRead()) {
      return StringUtils.EMPTY;
    }
    final char next = stringReader.peek();
    if (StringReader.isQuotedStringStart(next) || next == '/') {
      stringReader.skip();
      return stringReader.readStringUntil(next);
    }
    return readRegexUnquotedString(stringReader);
  }

  /**
   * 读取一个正则表达式，当正则表达式内容无效时，抛出异常。
   *
   * @throws CommandSyntaxException 如果正则表达式存在语法错误。
   */
  public static Pattern readRegex(StringReader stringReader) throws CommandSyntaxException {
    final int cursorAtRegexBegin = stringReader.getCursor();
    try {
      return Pattern.compile(readRegexString(stringReader));
    } catch (PatternSyntaxException e) {
      stringReader.setCursor(cursorAtRegexBegin);
      throw ModCommandExceptionTypes.INVALID_REGEX.createWithContext(stringReader, e.getMessage().replace(StringUtils.CR, StringUtils.EMPTY));
    }
  }

  public static String wrapPosition(Position position) {
    return position.getX() + " " + position.getY() + " " + position.getZ();
  }

  public static void expectAndSkipWhitespace(StringReader reader) throws CommandSyntaxException {
    if (!reader.canRead() || !Character.isWhitespace(reader.peek())) {
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, " ");
    }
    reader.skipWhitespace();
  }
}
