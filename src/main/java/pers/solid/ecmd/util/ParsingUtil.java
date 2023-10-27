package pers.solid.ecmd.util;

import com.google.common.base.Functions;
import com.google.common.base.Suppliers;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.chars.CharSet;
import net.minecraft.command.CommandSource;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.function.FailableSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 和命令解析和建议有关的实用方法。
 */
public final class ParsingUtil {
  private static final CharSet EXTENDED_ALLOWED_STRINGS = CharSet.of('!', '@', '#', '$', '%', '^', '&', '*', '?', '\\');

  private ParsingUtil() {
  }

  /**
   * 提供基于指定值的建议。这个值可以是任何类型，通过函数映射到字符串（提供建议）和文本组件（提供建议的提示，即 tooltip）。
   *
   * @param iterable   需要提供的值，可以是任何值。
   * @param suggestion 将值映射到字符串的函数，用于提供建议内容。
   * @param tooltip    将值映射到 {@link Message} 的函数，用于提供建议内容。
   */
  public static <T> CompletableFuture<Suggestions> suggestMatchingWithTooltip(Iterable<T> iterable, Function<T, String> suggestion, Function<T, Message> tooltip, SuggestionsBuilder builder) {
    final String remaining = builder.getRemainingLowerCase();
    for (T t : iterable) {
      final String candidate = suggestion.apply(t);
      if (CommandSource.shouldSuggest(remaining, candidate)) {
        builder.suggest(candidate, tooltip.apply(t));
      }
    }
    return builder.buildFuture();
  }

  /**
   * 提供基于指定的枚举值的建议，其中建议的内容由 {@link StringIdentifiable#asString()} 提供。
   */
  public static <T extends StringIdentifiable> CompletableFuture<Suggestions> suggestMatchingEnumWithTooltip(Iterable<T> enumIterable, Function<T, Message> tooltip, SuggestionsBuilder builder) {
    return suggestMatchingWithTooltip(enumIterable, StringIdentifiable::asString, tooltip, builder);
  }

  /**
   * 提供指定字符串的建议，并将字符串映射到文本组件以提供提示。
   *
   * @param candidates 需要建议的字符串。
   * @param tooltip    将字符串映射到 {@link Message} 以提供提示文本的函数。
   */
  public static CompletableFuture<Suggestions> suggestMatchingStringWithTooltip(Iterable<String> candidates, Function<String, Message> tooltip, SuggestionsBuilder builder) {
    return suggestMatchingWithTooltip(candidates, Functions.identity(), tooltip, builder);
  }

  public static CompletableFuture<Suggestions> suggestDirections(Iterable<Direction> directions, SuggestionsBuilder builder) {
    return suggestMatchingEnumWithTooltip(directions, TextUtil::wrapDirection, builder);
  }

  public static CompletableFuture<Suggestions> suggestDirections(SuggestionsBuilder builder) {
    return suggestDirections(Direction.stream()::iterator, builder);
  }

  /**
   * 提供单个字符串的建议（仅在字符串与输入的内容匹配时才建议），并通过 supplier 来指定提示文本。
   *
   * @param candidate 需要建议的字符串。
   * @param tooltip   该字符串对应的提示文本。
   */
  public static void suggestString(String candidate, Supplier<Message> tooltip, SuggestionsBuilder builder) {
    String remaining = builder.getRemainingLowerCase();
    if (CommandSource.shouldSuggest(remaining, candidate.toLowerCase())) {
      builder.suggest(candidate, tooltip.get());
    }
  }

  /**
   * 提供单个字符串的建议（仅在字符串与输入的内容匹配时才建议），不提供提示文本。
   *
   * @param candidate 需要建议的字符串。
   */
  public static void suggestString(String candidate, SuggestionsBuilder builder) {
    String remaining = builder.getRemainingLowerCase();
    if (CommandSource.shouldSuggest(remaining, candidate.toLowerCase())) {
      builder.suggest(candidate);
    }
  }

  /**
   * 提供单个字符串的建议（仅在字符串与输入的内容匹配时才建议），并指定提示文本。注意调用此函数时，
   *
   * @param candidate 需要建议的字符串。
   * @param tooltip   该字符串对应的提示文本。
   */
  public static void suggestString(String candidate, Message tooltip, SuggestionsBuilder builder) {
    suggestString(candidate, Suppliers.ofInstance(tooltip), builder);
  }

  /**
   * 通过指定的 {@link ArgumentType} 解析其对应的值并提供建议，用于使用 {@link SuggestedParser} 的情况。调用此函数时，会确保 {@link SuggestionsBuilder} 的位置合理。
   */
  public static <T> T suggestParserFromType(ArgumentType<T> argumentType, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final int cursorBeforeParse = parser.reader.getCursor();
    parser.suggestionProviders.add(SuggestionProvider.modifying((context, builder) -> {
      final SuggestionsBuilder builderOffset = builder.createOffset(cursorBeforeParse);
      return argumentType.listSuggestions(context, builderOffset);
    }));
    return argumentType.parse(parser.reader);
  }

  /**
   * 解析括号中的内容，并提供适当的建议。调用此方法前，parser 的 cursor 应当位于可能是括号的地方前面，当没有解析到括号时，返回 {@code null}。
   *
   * @param parseUnit 解析括号内的内容。
   * @throws CommandSyntaxException 当有左括号但缺失右括号时。
   */
  public static <T, E extends Throwable> @Nullable T parseParentheses(FailableSupplier<T, E> parseUnit, SuggestedParser parser) throws E, CommandSyntaxException {
    final StringReader reader = parser.reader;
    parser.suggestionProviders.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) suggestionsBuilder.suggest("(");
    });
    if (reader.canRead() && reader.peek() == '(') {
      reader.skip();
      reader.skipWhitespace();
      final T parse = parseUnit.get();
      parser.suggestionProviders.add((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) suggestionsBuilder.suggest(")");
      });
      reader.skipWhitespace();
      reader.expect(')');
      parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
      return parse;
    } else {
      return null;
    }
  }

  /**
   * 解析通过指定的间隔字符串（{@code joiningString}）分隔起来的一个或者多个值。
   *
   * @param parseUnit            解析每个单独的值的函数。
   * @param merger               当解析出来了多个 {@code T} 值时，将这多个 {@code T} 值合并为一个值。
   * @param joiningString        多个值之间用于间隔的间隔字符串。
   * @param joiningStringTooltip 当为 {@code joiningString} 提供建议时，应该显示的提示文本（tooltip）。
   * @param allowsSparse         是否允许各值与间隔字符串之间存在空白字符。
   */
  public static <T, E extends Throwable> T parseUnifiable(FailableSupplier<T, E> parseUnit, FailableFunction<List<T>, T, E> merger, String joiningString, Message joiningStringTooltip, SuggestedParser parser, boolean allowsSparse) throws E {
    final T first = parseUnit.get();
    final StringReader reader = parser.reader;
    final int cursorBeforeWhite = reader.getCursor();
    int cursorAfterLastUnit = cursorBeforeWhite;
    if (allowsSparse) reader.skipWhitespace();
    parser.suggestionProviders.add((context, suggestionsBuilder) -> suggestString(joiningString, joiningStringTooltip, suggestionsBuilder));
    if (reader.getString().startsWith(joiningString, reader.getCursor())) {
      final List<T> units = new ArrayList<>();
      units.add(first);
      while (reader.getString().startsWith(joiningString, reader.getCursor())) {
        reader.setCursor(reader.getCursor() + joiningString.length());
        parser.suggestionProviders.clear();
        if (allowsSparse) reader.skipWhitespace();
        units.add(parseUnit.get());
        cursorAfterLastUnit = reader.getCursor();
        if (allowsSparse) reader.skipWhitespace();
      }
      reader.setCursor(cursorAfterLastUnit);
      return merger.apply(units);
    } else {
      reader.setCursor(cursorBeforeWhite);
      return first;
    }
  }

  /**
   * 解析一个不同类型的角度时，并返回弧度值。角度值的单位可以是 {@code deg}、{@code rad} 或 {@code turn}。零值可以不提供单位，其他情况下不提供单位会抛出错误。输入完数值后会为单位提供建议。
   *
   * @param radians 返回的值是否为弧度值，若为 {@code false}，则返回角度值。
   */
  public static double parseAngle(SuggestedParser parser, boolean radians) throws CommandSyntaxException {
    final StringReader reader = parser.reader;
    final int cursorBeforeDouble = reader.getCursor();
    while (reader.canRead()) {
      final char peek = reader.peek();
      if ((peek < '0' || peek > '9') && peek != '-') {
        if (peek != '.') {
          break; // 无效字符
        } else if (reader.canRead(2) && reader.peek(1) == '.') {
          break; // 后面两个字符都是小数点，无效
        }
      }
      reader.skip();
    }
    final String substring = reader.getString().substring(cursorBeforeDouble, reader.getCursor());
    if (substring.isEmpty()) {
      parser.reader.setCursor(cursorBeforeDouble);
      parser.reader.readUnquotedString();
      final CommandSyntaxException exception = CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedDouble().createWithContext(reader);
      if (parser.reader.getCursor() > cursorBeforeDouble) {
        throw CommandSyntaxExceptionExtension.withCursorEnd(exception, parser.reader.getCursor());
      } else {
        throw exception;
      }
    }
    final double v;
    try {
      v = Double.parseDouble(substring);
    } catch (NumberFormatException e) {
      final int cursorAfterNumber = reader.getCursor();
      parser.reader.setCursor(cursorBeforeDouble);
      throw CommandSyntaxExceptionExtension.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidDouble().createWithContext(reader, substring), cursorAfterNumber);
    }
    parser.suggestionProviders.add((context, suggestionsBuilder) -> CommandSource.suggestMatching(List.of("deg", "rad", "turn"), suggestionsBuilder));
    final int cursorBeforeUnit = reader.getCursor();
    while (reader.canRead()) {
      final char peek = reader.peek();
      if (peek >= 'A' && peek <= 'Z' || peek >= 'a' && peek <= 'z') {
        reader.skip();
      } else {
        break;
      }
    }
    final String unit = reader.getString().substring(cursorBeforeUnit, reader.getCursor());
    if (unit.isEmpty()) {
      if (v == 0) {
        return 0;
      } else {
        reader.setCursor(cursorBeforeUnit);
        throw ModCommandExceptionTypes.ANGLE_UNIT_EXPECTED.createWithContext(reader, substring);
      }
    } else if ("deg".equals(unit)) {
      parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
      return radians ? Math.toRadians(v) : v;
    } else if ("rad".equals(unit)) {
      parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
      return radians ? v : Math.toDegrees(v);
    } else if ("turn".equals(unit)) {
      parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
      return (radians ? Math.PI * 2 : 360) * v;
    } else {
      final int cursorAfterUnit = reader.getCursor();
      reader.setCursor(cursorBeforeUnit);
      throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.ANGLE_UNIT_UNKNOWN.createWithContext(reader, unit), cursorAfterUnit);
    }
  }

  /**
   * 解析双精度浮点数的向量。这不是代表一个坐标，因此也不支持绝对坐标和局部坐标。形式为 {@code (<x> <y> <z> | [length] <direction>)}。
   */
  public static Vec3d parseVec3d(SuggestedParser parser) throws CommandSyntaxException {
    final StringReader reader = parser.reader;
    {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = reader.readUnquotedString();
      final Direction byName = Direction.byName(unquotedString);
      if (byName != null) {
        parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
        return Vec3d.of(byName.getVector());
      } else {
        reader.setCursor(cursorBeforeDirection);
      }
    }
    final double x = reader.readDouble();
    parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
    expectAndSkipWhitespace(reader);
    {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = reader.readUnquotedString();
      final Direction byName = Direction.byName(unquotedString);
      if (byName != null) {
        parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
        return Vec3d.of(byName.getVector()).multiply(x);
      } else {
        reader.setCursor(cursorBeforeDirection);
      }
    }
    final double y = reader.readDouble();
    parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
    expectAndSkipWhitespace(reader);
    final double z = reader.readDouble();
    return new Vec3d(x, y, z);
  }

  /**
   * 解析整数的向量。这不是代表一个坐标，因此也不支持绝对坐标和局部坐标。
   */
  public static Vec3i parseVec3i(SuggestedParser parser) throws CommandSyntaxException {
    final StringReader reader = parser.reader;
    {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = parser.reader.readUnquotedString();
      final Direction byName = Direction.byName(unquotedString);
      if (byName != null) {
        parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
        return byName.getVector();
      } else {
        parser.reader.setCursor(cursorBeforeDirection);
      }
    }
    final int x = reader.readInt();
    parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
    expectAndSkipWhitespace(reader);
    {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = parser.reader.readUnquotedString();
      final Direction byName = Direction.byName(unquotedString);
      if (byName != null) {
        parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
        return byName.getVector().multiply(x);
      } else {
        parser.reader.setCursor(cursorBeforeDirection);
      }
    }
    final int y = reader.readInt();
    parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
    expectAndSkipWhitespace(reader);
    final int z = reader.readInt();
    return new Vec3i(x, y, z);
  }

  /**
   * 判断字符串内的所有字符是否都可以在不带引号的字符串中出现。
   *
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

  /**
   * 解析并跳过空格。当没有空格时抛出错误。
   */
  public static void expectAndSkipWhitespace(StringReader reader) throws CommandSyntaxException {
    if (!reader.canRead() || !Character.isWhitespace(reader.peek())) {
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, " ");
    }
    reader.skipWhitespace();
  }
}
