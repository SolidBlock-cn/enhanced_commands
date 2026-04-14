package pers.solid.ecmd.nbt.predicate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.nbt.NbtParserShared;
import pers.solid.ecmd.nbt.function.PositionalListEntry;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.property.predicate.Comparator;
import pers.solid.ecmd.util.bridge.BridgeDoubleRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public final class NbtPredicateParser {
  public static final Component MATCH = Component.translatable("enhanced_commands.nbt_predicate.tooltip.match");
  public static final Component EQUAL = Component.translatable("enhanced_commands.nbt_predicate.tooltip.equal");
  public static final Component NOT_MATCH = Component.translatable("enhanced_commands.nbt_predicate.tooltip.not_match");
  public static final Component NOT_EQUAL = Component.translatable("enhanced_commands.nbt_predicate.tooltip.not_equal");
  public static final Component REGEX = Component.translatable("enhanced_commands.nbt_predicate.tooltip.regex");
  public static final Component NOT_REGEX = Component.translatable("enhanced_commands.nbt_predicate.tooltip.not_regex");
  public static final Component ANY_KEY = Component.translatable("enhanced_commands.nbt_predicate.tooltip.any_key");
  public static final Component ANY_VALUE = Component.translatable("enhanced_commands.nbt_predicate.tooltip.any_value");
  public static final SimpleCommandExceptionType SIGN_EXPECTED = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_predicate.sign_expected"));
  public static final DynamicCommandExceptionType DUPLICATE_KEY = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.nbt_predicate.duplicate_key", o));
  public static final DynamicCommandExceptionType MUST_BE_NUMBER_OR_STRING = new DynamicCommandExceptionType(actualType -> Component.translatable("enhanced_commands.nbt_predicate.must_be_number_or_string", actualType));
  public static final Component INTERSECT_TOOLTIP = Component.translatable("enhanced_commands.predicate.all.symbol_tooltip");
  public static final Component UNION_TOOLTIP = Component.translatable("enhanced_commands.predicate.any.symbol_tooltip");

  private NbtPredicateParser() {
  }

  private static CompletableFuture<Suggestions> suggestColonOrEqual(CommandContext<?> context, SuggestionsBuilder builder) {
    ParsingUtil.suggestString(":", MATCH, builder);
    ParsingUtil.suggestString("!:", NOT_MATCH, builder);
    ParsingUtil.suggestString("=", EQUAL, builder);
    ParsingUtil.suggestString("!=", NOT_EQUAL, builder);
    return builder.buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestCompoundAnyKey(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("*", ANY_KEY, suggestionsBuilder).buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestValueDifferentTypes(SuggestionsBuilder suggestionsBuilder) {
    ParsingUtil.suggestString("*", ANY_VALUE, suggestionsBuilder);
    ParsingUtil.suggestString("{", NbtParserShared.START_OF_COMPOUND, suggestionsBuilder);
    ParsingUtil.suggestString("[", NbtParserShared.START_OF_LIST, suggestionsBuilder);
    ParsingUtil.suggestString("(", suggestionsBuilder);
    ParsingUtil.suggestString("!", suggestionsBuilder);
    return suggestionsBuilder.buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestComparators(SuggestionsBuilder suggestionsBuilder) {
    return SharedSuggestionProvider.suggest(Arrays.stream(Comparator.values()).filter(comparator -> comparator != Comparator.EQ && comparator != Comparator.NE).map(Comparator::getSerializedName), suggestionsBuilder);
  }

  private static CompletableFuture<Suggestions> suggestWave(SuggestionsBuilder suggestionsBuilder) {
    ParsingUtil.suggestString("~", REGEX, suggestionsBuilder);
    ParsingUtil.suggestString("!~", NOT_REGEX, suggestionsBuilder);
    return suggestionsBuilder.buildFuture();
  }

  /**
   * <p>解析符号，并提供建议。这个符号可以是 {@code ":"}、{@code "="}。符号前面可以加个 {@code "!"} 以表示否定。
   * <p>在部分情况下，这个符号是可选的。例如，在列表中，每个元素的谓词可以不包含符号。但是，在复合标签中，在解析完键后，就必须要包含符号。
   *
   * @param mustExpectSign   是否必须要以 {@code ":"} 或者 {@code "="} 符号作为开头。
   * @param equalsForDefault 在没有符号作为前缀时，是否默认为 {@code "="}，而不是 {@code ":"}。
   */
  public static <S> byte parseSign(ParseContext<S> parseContext, boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    boolean isNegated = false;
    final StringReader reader = parseContext.reader();
    final int cursorBeforeSign = reader.getCursor();
    parseContext.addSuggestion(NbtPredicateParser::suggestColonOrEqual);
    if (!reader.canRead()) {
      if (mustExpectSign) {
        reader.setCursor(cursorBeforeSign);
        throw SIGN_EXPECTED.createWithContext(reader);
      } else {
        return (byte) (equalsForDefault ? 2 : 0);
      }
    }

    if (reader.peek() == '!') {
      isNegated = true;
      reader.skip();
      // 不 skipWhitespace，因为感叹号和后面的符号之间不接受空格，但是和值之间可以有。
    }
    boolean isUsingEqual = NbtParserShared.parseColonOrEqual(mustExpectSign, equalsForDefault, reader, cursorBeforeSign, SIGN_EXPECTED);
    parseContext.clearSuggestion();
    return (byte) ((isUsingEqual ? 2 : 0) + (isNegated ? 1 : 0));
  }

  /**
   * 解析复合标签。
   *
   * @see TagParser#readStruct()
   */
  public static <S> NbtPredicate parseCompound(ParseContext<S> parseContext, boolean isUsingEqual) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    NbtParserShared.handleCompoundStart(parseContext, reader);

    ListMultimap<String, NbtPredicate> entries = LinkedListMultimap.create();

    while (!reader.canRead() || reader.peek() != '}') {
      reader.skipWhitespace();
      int cursorBeforeKey = reader.getCursor();
      parseContext.setSuggestion(NbtPredicateParser::suggestCompoundAnyKey);
      final String key;
      if (!reader.canRead()) {
        throw TagParser.ERROR_EXPECTED_KEY.createWithContext(reader);
      } else if (!isUsingEqual && reader.peek() == '*') {
        key = null;
        reader.skip();
      } else {
        key = reader.readString();
      }
      if (key != null && key.isEmpty()) {
        reader.setCursor(cursorBeforeKey);
        throw TagParser.ERROR_EXPECTED_KEY.createWithContext(reader);
      }

      reader.skipWhitespace();
      entries.put(key, parseNbtPredicate(parseContext, true, false));

      if (NbtParserShared.handleCompoundSeparate(parseContext, reader)) {
        break;
      }
    }

    NbtParserShared.handleCompoundEnd(parseContext, reader);
    if (isUsingEqual) {
      try {
        return new EqualsCompoundNbtPredicate(entries.entries().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
      } catch (IllegalArgumentException e) {
        throw DUPLICATE_KEY.create("");
      }
    } else {
      return new MatchCompoundNbtPredicate(entries);
    }
  }

  /**
   * 解析列表。
   *
   * @see TagParser#readListTag()
   */
  public static <S> NbtPredicate parseList(ParseContext<S> parseContext, boolean isUsingEqual) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    NbtParserShared.handListStart(parseContext, reader);

    final List<NbtPredicate> expected = new ArrayList<>();
    final List<PositionalListEntry<NbtPredicate>> expectedPositional = isUsingEqual ? null : new ArrayList<>();
    while (!reader.canRead() || reader.peek() != ']') {
      int cursorBeforeListElement = reader.getCursor();
      boolean isUsingPositionalPredicate = false;
      // 对于 isUsingEqual = false 的情况，尝试读取带有键的列表元素谓词
      // 例如：[2: "abc", 5 = "cde"]
      @Nullable CommandSyntaxException exceptionWhenParsingPositionalPredicate = null;
      int cursorWhenParsingPositionalPredicate = -1;
      @Nullable List<SuggestionProvider<S>> suggestionsWhenParsingPositionalPredicate = null;
      if (!isUsingEqual && reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
        try {
          final int index = reader.readInt();
          reader.skipWhitespace();
          final NbtPredicate nbtPredicate = parseNbtPredicate(parseContext, true, false);
          expectedPositional.add(new PositionalListEntry<>(index, nbtPredicate));
          isUsingPositionalPredicate = true;
        } catch (CommandSyntaxException e) {
          cursorWhenParsingPositionalPredicate = reader.getCursor();
          exceptionWhenParsingPositionalPredicate = e;
          suggestionsWhenParsingPositionalPredicate = parseContext.getAllSuggestions();
          reader.setCursor(cursorBeforeListElement);
        }
      }
      if (!isUsingPositionalPredicate) {
        try {
          final NbtPredicate nbtPredicate = parseNbtPredicate(parseContext, false, isUsingEqual);
          expected.add(nbtPredicate);
        } catch (CommandSyntaxException exception) {
          if (exceptionWhenParsingPositionalPredicate != null) {
            reader.setCursor(cursorWhenParsingPositionalPredicate);
            parseContext.replaceAllSuggestions(suggestionsWhenParsingPositionalPredicate);
            throw exceptionWhenParsingPositionalPredicate;
          } else {
            throw exception;
          }
        }
      }

      reader.skipWhitespace();
      if (NbtParserShared.handleListSeparate(parseContext, reader)) {
        break;
      }
    }

    NbtParserShared.handleListEnd(parseContext, reader);
    return isUsingEqual ? new EqualsListNbtPredicate(expected) : new MatchListNbtPredicate(expected, expectedPositional);
  }

  public static <S> NbtPredicate parseNbtPredicate(ParseContext<S> parseContext, boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorBeforeSign = reader.getCursor();
    // 尝试读取正则表达式语法
    parseContext.addSuggestion((context, suggestionsBuilder) -> {
          final SuggestionsBuilder offset = suggestionsBuilder.createOffset(cursorBeforeSign);
          suggestWave(offset);
          suggestComparators(offset);

          if (!mustExpectSign) {
            suggestValueDifferentTypes(offset);
          }
          return offset.buildFuture();
        }
    );
    if ((reader.canRead() && reader.peek() == '~') || (reader.canRead(2) && reader.peek() == '!' && reader.peek(1) == '~')) {
      final boolean isNegated = reader.peek() == '!';
      reader.skip();
      if (isNegated) {
        reader.skip();
      }
      // 开始解析字符串，并将其视为正则表达式
      parseContext.clearSuggestion();
      reader.skipWhitespace();
      return parseRegexUnion(parseContext);
    }

    // 尝试读取比较值（除了等号和不等号之外的值）

    for (Comparator comparator : Comparator.values()) {
      if (comparator != Comparator.EQ && comparator != Comparator.NE) {
        final String name = comparator.getSerializedName();
        if (reader.getRemaining().startsWith(name)) {
          // 防止这种情况：“<=”开头的被解析为“>”
          if (comparator == Comparator.LT || comparator == Comparator.GT) {
            if (reader.canRead(2) && reader.peek(1) == '=') {
              continue;
            }
          }

          // 解析完成了符号，后面应该是数字或者字符串
          reader.setCursor(reader.getCursor() + name.length());
          parseContext.clearSuggestion();
          reader.skipWhitespace();

          // 尝试读取一个数字或字符串
          if (!reader.canRead()) {
            throw TagParser.ERROR_EXPECTED_VALUE.createWithContext(reader);
          } else if (reader.peek() == '{') {
            throw MUST_BE_NUMBER_OR_STRING.createWithContext(reader, CompoundTag.TYPE.getPrettyName());
          } else if (reader.peek() == '[') {
            throw MUST_BE_NUMBER_OR_STRING.createWithContext(reader, ListTag.TYPE.getPrettyName());
          }
          final Tag element = new TagParser(reader).readValue();
          if (element instanceof StringTag || element instanceof NumericTag) {
            return new ComparisonNbtPredicate(comparator, element);
          } else {
            throw MUST_BE_NUMBER_OR_STRING.createWithContext(reader, element.getType().getPrettyName());
          }
        }
      }
    }

    // 解析等号和不等号
    final byte b = parseSign(parseContext, mustExpectSign, equalsForDefault);
    final boolean isUsingEqual = ((b >> 1) & 1) == 1;
    final boolean isNegated = (b & 1) == 1;

    final boolean hasExplicitSign = cursorBeforeSign != reader.getCursor();
    reader.skipWhitespace();
    if (hasExplicitSign) {
      parseContext.clearSuggestion();
    }
    final NbtPredicate nbtPredicate = NbtPredicateParser.parseAfterOrdinalSign(parseContext, isUsingEqual);
    return isNegated ? nbtPredicate.negate() : nbtPredicate;
  }

  private static <S> NbtPredicate parseAfterOrdinalSign(ParseContext<S> parseContext, boolean isUsingEqual) throws CommandSyntaxException {
    return NbtPredicateParser.parseUnion(parseContext, isUsingEqual);
  }

  public static <S> NbtPredicate parseUnion(ParseContext<S> parseContext, boolean isUsingEqual) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> NbtPredicateParser.parseIntersect(parseContext, isUsingEqual), AnyNbtPredicate::new, "|", UNION_TOOLTIP, parseContext);
  }

  public static <S> NbtPredicate parseIntersect(ParseContext<S> parseContext, boolean isUsingEqual) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> NbtPredicateParser.parseUnit(parseContext, isUsingEqual), AllNbtPredicate::new, "&", INTERSECT_TOOLTIP, parseContext);
  }

  private static <S> NbtPredicate parseUnit(ParseContext<S> parseContext, boolean isUsingEqual) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorBeforeUnit = reader.getCursor();
    final NbtPredicate functionGrammar = NbtPredicateParser.parseFunctionGrammar(parseContext, reader);
    parseContext.addSuggestion((context, builder) -> suggestValueDifferentTypes(builder.createOffset(cursorBeforeUnit)));
    if (functionGrammar != null) {
      return functionGrammar;
    }

    if (!reader.canRead()) {
      throw TagParser.ERROR_EXPECTED_VALUE.createWithContext(reader);
    }
    if (reader.peek() == '*') {
      reader.skip();
      parseContext.clearSuggestion();
      return ConstantNbtPredicate.of(true);
    } else if (reader.peek() == '{') {
      return parseCompound(parseContext.withAllowSparse(true), isUsingEqual);
    } else if (reader.peek() == '[' && !(reader.canRead(3) && !StringReader.isQuotedStringStart(reader.peek()) && reader.peek(2) == ';')) {
      return parseList(parseContext.withAllowSparse(true), isUsingEqual);
    } else if (reader.peek() == '(') {
      final ParseContext<S> parseContextSparse = parseContext.withAllowSparse(true);
      return ParsingUtil.parseParentheses(() -> parseNbtPredicate(parseContextSparse, false, isUsingEqual), parseContextSparse);
    } else if (reader.peek() == '!') {
      reader.skip();
      reader.skipWhitespace();
      return parseUnit(parseContext, isUsingEqual).negate();
    } else {
      // 先尝试读取 NumberRange
      final int cursorBeforeRange = reader.getCursor();
      try {
        final BridgeDoubleRange parsedRange = BridgeDoubleRange.parse(reader);
        if (parsedRange.min != null && !parsedRange.min.equals(parsedRange.max)) {
          return new RangeNbtPredicate(parsedRange);
        } else {
          reader.setCursor(cursorBeforeRange);
        }
      } catch (CommandSyntaxException ignored) {
      }

      final Tag element = new TagParser(reader).readValue();
      if (isUsingEqual) {
        if (element instanceof NumericTag nbtNumber) {
          return new ComparisonNbtPredicate(Comparator.EQ, nbtNumber);
        }
      }
      return new MatchPrimitiveNbtPredicate(element);
    }
  }

  public static <S> NbtPredicate parseRegexUnion(ParseContext<S> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> NbtPredicateParser.parseRegexIntersect(parseContext), AnyNbtPredicate::new, "|", UNION_TOOLTIP, parseContext);
  }

  public static <S> NbtPredicate parseRegexIntersect(ParseContext<S> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> NbtPredicateParser.parseRegexUnit(parseContext), AllNbtPredicate::new, "&", INTERSECT_TOOLTIP, parseContext);
  }

  private static <S> NbtPredicate parseRegexUnit(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    if (reader.canRead() && reader.peek() == '!') {
      reader.skip();
      reader.skipWhitespace();
      final Pattern pattern = ParsingUtil.readRegex(reader);
      return new NegatingNbtPredicate(new RegexNbtPredicate(pattern));
    } else if (reader.canRead() && reader.peek() == '(') {
      final ParseContext<S> parseContextSparse = parseContext.withAllowSparse(true);
      return ParsingUtil.parseParentheses(() -> parseRegexUnion(parseContextSparse), parseContextSparse);
    } else {
      return new RegexNbtPredicate(ParsingUtil.readRegex(reader));
    }
  }

  private static @Nullable <S> NbtPredicate parseFunctionGrammar(ParseContext<S> parseContext, StringReader reader) throws CommandSyntaxException {
    final int cursorBeforeFunctionName = reader.getCursor();
    final NbtPredicate functionGrammar = NbtPredicateParsing.FUNCTIONS_PARSER.parse(parseContext.withAllowSparse(true));
    if (functionGrammar != null) {
      return functionGrammar;
    } else {
      if (reader.getCursor() != cursorBeforeFunctionName) {
        parseContext.terminateSuggestionsIfNotEmpty();
      }
      reader.setCursor(cursorBeforeFunctionName);
    }
    return null;
  }
}
