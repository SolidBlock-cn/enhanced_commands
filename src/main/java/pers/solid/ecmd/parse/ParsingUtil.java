package pers.solid.ecmd.parse;

import com.google.common.base.Functions;
import com.google.common.base.Suppliers;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.chars.CharSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.Util;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.function.FailableSupplier;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.accessor.ParserUtilsAccessor;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 此类包含与命令解析和建议有关的静态实用方法。
 */
public final class ParsingUtil {
  public static final DynamicCommandExceptionType UNKNOWN_VALUE = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.argument.unknown_value", o));
  private static final CharSet EXTENDED_ALLOWED_STRINGS = CharSet.of('!', '@', '#', '$', '%', '^', '&', '*', '?', '\\');

  private ParsingUtil() {
  }

  /**
   * 提供基于指定的枚举值的建议，其中建议的内容由 {@link StringRepresentable#getSerializedName()} 提供。
   */
  public static <T extends StringRepresentable> CompletableFuture<Suggestions> suggestMatchingEnumWithTooltip(Iterable<T> enumIterable, Function<T, Message> tooltip, SuggestionsBuilder builder) {
    return SharedSuggestionProvider.suggest(enumIterable, builder, StringRepresentable::getSerializedName, tooltip);
  }

  /**
   * 提供指定字符串的建议，并将字符串映射到文本组件以提供提示。
   *
   * @param candidates 需要建议的字符串。
   * @param tooltip    将字符串映射到 {@link Message} 以提供提示文本的函数。
   */
  public static CompletableFuture<Suggestions> suggestMatchingStringWithTooltip(Iterable<String> candidates, Function<String, Message> tooltip, SuggestionsBuilder builder) {
    return SharedSuggestionProvider.suggest(candidates, builder, Functions.identity(), tooltip);
  }

  public static CompletableFuture<Suggestions> suggestDirections(Iterable<Direction> directions, SuggestionsBuilder builder) {
    return suggestMatchingEnumWithTooltip(directions, TextUtil::wrapDirection, builder);
  }

  public static CompletableFuture<Suggestions> suggestDirections(SuggestionsBuilder builder) {
    return suggestDirections(Direction.stream()::iterator, builder);
  }

  /**
   * 在输入布尔值时，提供布尔值的建议。
   */
  public static CompletableFuture<Suggestions> suggestBoolean(SuggestionsBuilder builder) {
    return SharedSuggestionProvider.suggest(new String[]{"true", "false"}, builder);
  }

  /**
   * 提供单个字符串的建议（仅在字符串与输入的内容匹配时才建议），并通过 supplier 来指定提示文本。
   *
   * @param candidate 需要建议的字符串。
   * @param tooltip   该字符串对应的提示文本。
   */
  public static SuggestionsBuilder suggestString(String candidate, Supplier<Message> tooltip, SuggestionsBuilder builder) {
    String remaining = builder.getRemainingLowerCase();
    if (SharedSuggestionProvider.matchesSubStr(remaining, candidate.toLowerCase(Locale.ROOT))) {
      builder.suggest(candidate, tooltip.get());
    }
    return builder;
  }

  /**
   * 提供单个字符串的建议（仅在字符串与输入的内容匹配时才建议），不提供提示文本。
   *
   * @param candidate 需要建议的字符串。
   */
  public static SuggestionsBuilder suggestString(String candidate, SuggestionsBuilder builder) {
    String remaining = builder.getRemainingLowerCase();
    if (SharedSuggestionProvider.matchesSubStr(remaining, candidate.toLowerCase(Locale.ROOT))) {
      builder.suggest(candidate);
    }
    return builder;
  }

  /**
   * 提供单个字符串的建议（仅在字符串与输入的内容匹配时才建议），并指定提示文本。注意调用此函数时，
   *
   * @param candidate 需要建议的字符串。
   * @param tooltip   该字符串对应的提示文本。
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
  public static <T extends @Nullable Object, E extends Throwable> @Nullable T parseParentheses(FailableSupplier<T, E> parseUnit, ParseContext<?> parseContext) throws E, CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    parseContext.addSuggestion((context, suggestionsBuilder) -> suggestString("(", suggestionsBuilder).buildFuture());
    if (reader.canRead() && reader.peek() == '(') {
      reader.skip();
      reader.skipWhitespace();
      parseContext.clearSuggestion();
      final T parse = parseUnit.get();
      parseContext.addSuggestion((context, suggestionsBuilder) -> suggestString(")", suggestionsBuilder).buildFuture());
      reader.skipWhitespace();
      reader.expect(')');
      parseContext.clearSuggestion();
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
   */
  public static <T, E extends Throwable, S> T parseUnifiable(FailableSupplier<T, E> parseUnit, FailableFunction<List<T>, T, E> merger, String joiningString, Message joiningStringTooltip, ParseContext<S> parseContext) throws E {
    final T first = parseUnit.get();
    final StringReader reader = parseContext.reader();
    final int cursorBeforeWhite = reader.getCursor();
    int cursorAfterLastUnit = cursorBeforeWhite;
    if (parseContext.allowSparse()) reader.skipWhitespace();
    final SuggestionProvider<S> joiningStringSuggestion = (context, suggestionsBuilder) -> suggestString(joiningString, joiningStringTooltip, suggestionsBuilder).buildFuture();
    parseContext.addSuggestion(joiningStringSuggestion);
    if (reader.getString().startsWith(joiningString, reader.getCursor())) {
      final List<T> units = new ArrayList<>();
      units.add(first);
      while (reader.getString().startsWith(joiningString, reader.getCursor())) {
        reader.setCursor(reader.getCursor() + joiningString.length());
        parseContext.clearSuggestion();
        if (parseContext.allowSparse()) reader.skipWhitespace();
        units.add(parseUnit.get());
        cursorAfterLastUnit = reader.getCursor();
        if (parseContext.allowSparse()) reader.skipWhitespace();
        parseContext.addSuggestion(joiningStringSuggestion);
      }
      reader.setCursor(cursorAfterLastUnit);
      return merger.apply(units);
    } else {
      reader.setCursor(cursorBeforeWhite);
      return first;
    }
  }

  /**
   * 解析一个字符串值，并根据字符串值转换为特定的对象。如果特定的对象为 {@code null}，则抛出异常。
   *
   * @see ParseContext#parseAndSuggestValues
   */
  public static <T> T parseValues(StringReader reader, FailableFunction<String, @Nullable T, CommandSyntaxException> valueGetter) throws CommandSyntaxException {
    final int cursorBeforeRead = reader.getCursor();
    final String name = reader.readString();
    final int cursorAfterRead = reader.getCursor();
    final T value = valueGetter.apply(name);
    if (value == null) {
      reader.setCursor(cursorBeforeRead);
      throw EnhancedCommandSyntaxException.withCursorEnd(UNKNOWN_VALUE.createWithContext(reader, name), cursorAfterRead);
    } else {
      return value;
    }
  }

  /**
   * 判断字符串内的所有字符是否都可以在不带引号的字符串中出现。
   *
   * @see StringReader#isAllowedInUnquotedString(char)
   */
  public static boolean isAllowedInUnquotedString(final String s) {
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (!StringReader.isAllowedInUnquotedString(c))
        return false;
    }
    return true;
  }

  /**
   * 尝试使用不带引号的形式表示字符串，如果不行，则带上引号。
   */
  public static String quoteStringIfNeeded(final String s) {
    if (isAllowedInUnquotedString(s)) {
      return s;
    } else {
      return StringTag.quoteAndEscape(s);
    }
  }

  /**
   * 此方法可以接受更多的字符串，常用于正则表达式。但是，仍然不接受括号。
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
      throw EnhancedCommandsCommandExceptionTypes.INVALID_REGEX.createWithContext(stringReader, e.getMessage().replace(StringUtils.CR, StringUtils.EMPTY));
    }
  }

  /**
   * 解析并跳过空格。当没有空格时抛出异常。
   */
  public static void expectAndSkipWhitespace(StringReader reader) throws CommandSyntaxException {
    if (!reader.canRead() || !Character.isWhitespace(reader.peek())) {
      throw EnhancedCommandsCommandExceptionTypes.EXPECTED_WHITESPACE.createWithContext(reader);
    }
    reader.skipWhitespace();
  }

  /**
   * 通过反射的方式，从 {@link JsonReader} 中读取位置信息。
   */
  public static int getPos(JsonReader jsonReader) {
    return ParserUtilsAccessor.invokeGetPos(jsonReader) - 1;
  }

  public static <A, E extends Throwable> A parseNbt(StringReader reader, FailableFunction<Tag, A, E> readFunction) throws E, CommandSyntaxException {
    final Tag nbtElement = new TagParser(reader).readValue();
    return readFunction.apply(nbtElement);
  }

  public static <A, E extends Throwable> A parseNbt(StringReader reader, Codec<A> codec, Function<String, E> exceptionSupplier) throws E, CommandSyntaxException {
    return parseNbt(reader, element -> codec.parse(NbtOps.INSTANCE, element).getOrThrow(exceptionSupplier));
  }

  public static <T> void registerNameSuggestionProvider(ResourceKey<? extends Registry<T>> registryKey, Function<? super Holder.Reference<T>, ? extends Message> function) {
    NameSuggestionsInitHolder.NAME_SUGGESTION_PROVIDERS.put(registryKey, function);
  }

  @SuppressWarnings("unchecked")
  public static <T> @Nullable Function<? super Holder.Reference<T>, ? extends Message> getNameSuggestionProvider(ResourceKey<? extends Registry<T>> registryKey) {
    return (Function<? super Holder.Reference<T>, ? extends Message>) NameSuggestionsInitHolder.NAME_SUGGESTION_PROVIDERS.get(registryKey);
  }

  /**
   * <p>读取 1~3 个数（由空格分隔），并根据这 1~3 个数组成一个 {@code Vec3d}。</p>
   * <p>当第 3 个数不存在时，z 采用第 1 个数。当第 2 个数不存在时，y 采用第 1 个数。例如：</p>
   * <ul>
   *   <li>{@code 1} → {@code (1, 1, 1)}</li>
   *   <li>{@code 1, 2} -> {@code (1, 2, 1)}</li>
   *   <li>{@code 1, 2, 3} -> {@code (1, 2, 3)}</li>
   * </ul>
   */
  public static Vec3 parseShortenableVec3d(StringReader reader) throws CommandSyntaxException {
    final double x = reader.readDouble();
    final int beforeFirstWhite = reader.getCursor();
    reader.skipWhitespace();
    if (reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
      final double y = reader.readDouble();
      final int beforeSecondWhite = reader.getCursor();
      reader.skipWhitespace();
      if (reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
        final double z = reader.readDouble();
        return new Vec3(x, y, z);
      } else {
        reader.setCursor(beforeSecondWhite);
        return new Vec3(x, y, x);
      }
    } else {
      reader.setCursor(beforeFirstWhite);
      return new Vec3(x, x, x);
    }
  }

  private static class NameSuggestionsInitHolder {
    private static final Map<ResourceKey<? extends Registry<?>>, Function<?, ? extends Message>> NAME_SUGGESTION_PROVIDERS = new Reference2ReferenceOpenHashMap<>();

    static {
      initDefaultSuggestionProviders();
    }

    private static void initDefaultSuggestionProviders() {
      registerNameSuggestionProvider(Registries.BIOME, ref -> Component.translatable(Util.makeDescriptionId("biome", ref.key().location())));
      registerNameSuggestionProvider(Registries.BLOCK, Functions.compose(Block::getName, Holder.Reference::value));
      registerNameSuggestionProvider(Registries.ITEM, Functions.compose(Item::getDescription, Holder.Reference::value));
      registerNameSuggestionProvider(Registries.ENTITY_TYPE, Functions.compose(EntityType::getDescription, Holder.Reference::value));
      registerNameSuggestionProvider(Registries.MOB_EFFECT, Functions.compose(MobEffect::getDisplayName, Holder.Reference::value));
    }
  }
}
