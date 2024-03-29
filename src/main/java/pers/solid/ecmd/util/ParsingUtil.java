package pers.solid.ecmd.util;

import com.google.common.base.Functions;
import com.google.common.base.Suppliers;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.chars.CharSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.function.FailableSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.mixin.TextSerializerAccessor;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 此类包含与命令解析和建议有关的静态实用方法。
 */
public final class ParsingUtil {
  private static final CharSet EXTENDED_ALLOWED_STRINGS = CharSet.of('!', '@', '#', '$', '%', '^', '&', '*', '?', '\\');

  private ParsingUtil() {
  }

  /**
   * 提供基于指定的枚举值的建议，其中建议的内容由 {@link StringIdentifiable#asString()} 提供。
   */
  public static <T extends StringIdentifiable> CompletableFuture<Suggestions> suggestMatchingEnumWithTooltip(Iterable<T> enumIterable, Function<T, Message> tooltip, SuggestionsBuilder builder) {
    return CommandSource.suggestMatching(enumIterable, builder, StringIdentifiable::asString, tooltip);
  }

  /**
   * 提供指定字符串的建议，并将字符串映射到文本组件以提供提示。
   *
   * @param candidates 需要建议的字符串。
   * @param tooltip    将字符串映射到 {@link Message} 以提供提示文本的函数。
   */
  public static CompletableFuture<Suggestions> suggestMatchingStringWithTooltip(Iterable<String> candidates, Function<String, Message> tooltip, SuggestionsBuilder builder) {
    return CommandSource.suggestMatching(candidates, builder, Functions.identity(), tooltip);
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
   * @return
   */
  public static SuggestionsBuilder suggestString(String candidate, Supplier<Message> tooltip, SuggestionsBuilder builder) {
    String remaining = builder.getRemainingLowerCase();
    if (CommandSource.shouldSuggest(remaining, candidate.toLowerCase(Locale.ROOT))) {
      builder.suggest(candidate, tooltip.get());
    }
    return builder;
  }

  /**
   * 提供单个字符串的建议（仅在字符串与输入的内容匹配时才建议），不提供提示文本。
   *
   * @param candidate 需要建议的字符串。
   * @return
   */
  public static SuggestionsBuilder suggestString(String candidate, SuggestionsBuilder builder) {
    String remaining = builder.getRemainingLowerCase();
    if (CommandSource.shouldSuggest(remaining, candidate.toLowerCase(Locale.ROOT))) {
      builder.suggest(candidate);
    }
    return builder;
  }

  /**
   * 提供单个字符串的建议（仅在字符串与输入的内容匹配时才建议），并指定提示文本。注意调用此函数时，
   *
   * @param candidate 需要建议的字符串。
   * @param tooltip   该字符串对应的提示文本。
   * @return
   */
  public static SuggestionsBuilder suggestString(String candidate, Message tooltip, SuggestionsBuilder builder) {
    return suggestString(candidate, Suppliers.ofInstance(tooltip), builder);
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
   * 尝试使用不带引号的形式示字符串，如果不行，则带上引号。
   */
  public static String quoteStringIfNeeded(final @NotNull String s) {
    if (isAllowedInUnquotedString(s)) {
      return s;
    } else {
      return NbtString.escape(s);
    }
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
      throw ModCommandExceptionTypes.EXPECTED_WHITESPACE.createWithContext(reader);
    }
    reader.skipWhitespace();
  }

  /**
   * 通过反射的方式，从 {@link JsonReader} 中读取位置信息。
   */
  public static int getPos(@NotNull JsonReader jsonReader) {
    return TextSerializerAccessor.invokeGetPosition(jsonReader) - 1;
  }

  public static <T> T parseJson(StringReader reader, FailableFunction<JsonReader, T, JsonParseException> readFunction, DynamicCommandExceptionType exceptionType) throws CommandSyntaxException {
    final java.io.StringReader in = new java.io.StringReader(reader.getString());
    final int cursorBeforeJson = reader.getCursor();
    try {
      in.skip(cursorBeforeJson);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    final JsonReader jsonReader = new JsonReader(in);
    try {
      final T apply = readFunction.apply(jsonReader);
      reader.setCursor(cursorBeforeJson + getPos(jsonReader));
      return apply;
    } catch (RuntimeException e) {
      if (e.getCause() instanceof MalformedJsonException malformedJsonException) {
        reader.setCursor(cursorBeforeJson + getPos(jsonReader) - 1);
        throw ModCommandExceptionTypes.MALFORMED_JSON.createWithContext(reader, malformedJsonException.getMessage());
      } else {
        throw CommandSyntaxExceptionExtension.withCursorEnd(exceptionType.createWithContext(reader, e.getMessage()), cursorBeforeJson + getPos(jsonReader));
      }
    }
  }

  private static final Reference2ReferenceMap<RegistryKey<? extends Registry<?>>, Function<?, ? extends Message>> NAME_SUGGESTION_PROVIDERS = new Reference2ReferenceOpenHashMap<>();

  public static <T> void registerNameSuggestionProvider(RegistryKey<? extends Registry<T>> registryKey, Function<? super T, ? extends Message> function) {
    NAME_SUGGESTION_PROVIDERS.put(registryKey, function);
  }

  @SuppressWarnings("unchecked")
  public static <T> Function<? super T, ? extends Message> getNameSuggestionProvider(RegistryKey<? extends Registry<T>> registryKey) {
    NameSuggestionsInitHolder.makeSureInitialized();
    return (Function<? super T, ? extends Message>) NAME_SUGGESTION_PROVIDERS.get(registryKey);
  }

  private static void initDefaultSuggestionProviders() {
    registerNameSuggestionProvider(RegistryKeys.BLOCK, Block::getName);
    registerNameSuggestionProvider(RegistryKeys.ITEM, Item::getName);
    registerNameSuggestionProvider(RegistryKeys.ENTITY_TYPE, EntityType::getName);
    registerNameSuggestionProvider(RegistryKeys.STATUS_EFFECT, StatusEffect::getName);
  }

  private static class NameSuggestionsInitHolder {
    private static void makeSureInitialized() {
    }

    static {
      initDefaultSuggestionProviders();
    }
  }
}
