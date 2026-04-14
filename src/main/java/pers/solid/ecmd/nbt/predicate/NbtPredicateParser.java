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

public class NbtPredicateParser<S> {
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
  private final ParseContext<S> parseContext;

  public NbtPredicateParser(ParseContext<S> parseContext) {
    this.parseContext = parseContext;
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
  public byte parseSign(boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    boolean isNegated = false;
    final StringReader reader = parseContext.reader();
    final int cursorBeforeSign = reader.getCursor();
    parseContext.setSuggestion(NbtPredicateParser::suggestColonOrEqual);
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
  public NbtPredicate parseCompound(boolean isUsingEqual, boolean isNegated) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    parseContext.setSuggestion(NbtParserShared::suggestCompoundStart);
    reader.expect('{');
    parseContext.clearSuggestion();
    reader.skipWhitespace();
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
      entries.put(key, parseNbtPredicate(true, false));
      parseContext.terminateSuggestionsIfNotEmpty();
      parseContext.addSuggestion(NbtParserShared::suggestCompoundSeparate);
      if (reader.canRead() && reader.peek() == ',') {
        reader.skip();
        parseContext.clearSuggestion();
        reader.skipWhitespace();
      } else {
        break;
      }
    }

    reader.skipWhitespace();
    parseContext.terminateSuggestionsIfNotEmpty();
    parseContext.addSuggestion(NbtParserShared::suggestCompoundEnd);
    reader.expect('}');
    parseContext.clearSuggestion();
    if (isUsingEqual) {
      try {
        return new EqualsCompoundNbtPredicate(entries.entries().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)), isNegated);
      } catch (IllegalArgumentException e) {
        throw DUPLICATE_KEY.create("");
      }
    } else {
      return new MatchCompoundNbtPredicate(entries, isNegated);
    }
  }

  /**
   * 解析列表。
   *
   * @see TagParser#readListTag()
   */
  public NbtPredicate parseList(boolean isUsingEqual, boolean isNegated) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    reader.expect('[');
    reader.skipWhitespace();
    final List<NbtPredicate> expected = new ArrayList<>();
    final List<PositionalListEntry<NbtPredicate>> expectedPositional = isUsingEqual ? null : new ArrayList<>();

    parseContext.addSuggestion(NbtParserShared::suggestListEnd);
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
          final NbtPredicate nbtPredicate = parseNbtPredicate(true, false);
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
          final NbtPredicate nbtPredicate = parseNbtPredicate(false, isUsingEqual);
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
      parseContext.terminateSuggestionsIfNotEmpty();
      parseContext.addSuggestion(NbtParserShared::suggestListSeparate);
      if (reader.canRead() && reader.peek() == ',') {
        reader.skip();
        parseContext.clearSuggestion();
        reader.skipWhitespace();
      } else {
        break;
      }
    }

    reader.skipWhitespace();
    parseContext.addSuggestion(NbtParserShared::suggestListEnd);
    reader.expect(']');
    parseContext.clearSuggestion();
    return isUsingEqual ? new EqualsListNbtPredicate(expected, isNegated) : new MatchListNbtPredicate(expected, expectedPositional, isNegated);
  }

  public NbtPredicate parseNbtPredicate(boolean mustExpectSign, boolean equalsForDefault)
      throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorBeforeSign = reader.getCursor();
    // 尝试读取正则表达式语法
    parseContext.addSuggestion((context, suggestionsBuilder) -> {
          final SuggestionsBuilder offset = suggestionsBuilder.createOffset(cursorBeforeSign);
          suggestWave(offset);
          suggestComparators(offset);
          return suggestValueDifferentTypes(offset);
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
      return new RegexNbtPredicate(ParsingUtil.readRegex(reader), false);
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
    final byte b = parseSign(mustExpectSign, equalsForDefault);
    final boolean isUsingEqual = ((b >> 1) & 1) == 1;
    final boolean isNegated = (b & 1) == 1;


    final boolean hasExplicitSign = cursorBeforeSign != reader.getCursor();
    reader.skipWhitespace();
    if (hasExplicitSign) {
      parseContext.clearSuggestion();
    }

    final NbtPredicate functionGrammar = parseFunctionGrammar(reader);
    if (functionGrammar != null) {
      return isNegated ? new NegatingNbtPredicate(functionGrammar) : functionGrammar;
    }

    if (!reader.canRead()) {
      throw TagParser.ERROR_EXPECTED_VALUE.createWithContext(reader);
    }
    if (reader.peek() == '*') {
      reader.skip();
      parseContext.clearSuggestion();
      return ConstantNbtPredicate.of(!isNegated);
    } else if (reader.peek() == '{') {
      return parseCompound(isUsingEqual, isNegated);
    } else if (reader.peek() == '[' && !(reader.canRead(3) && !StringReader.isQuotedStringStart(reader.peek()) && reader.peek(2) == ';')) {
      return parseList(isUsingEqual, isNegated);
    } else {

      // 先尝试读取 NumberRange
      final int cursorBeforeRange = reader.getCursor();
      try {
        final BridgeDoubleRange parsedRange = BridgeDoubleRange.parse(reader);
        if (parsedRange.min != null && !parsedRange.min.equals(parsedRange.max)) {
          return new RangeNbtPredicate(parsedRange, isNegated);
        } else {
          reader.setCursor(cursorBeforeRange);
        }
      } catch (CommandSyntaxException ignored) {
      }

      final Tag element = new TagParser(reader).readValue();
      if (isUsingEqual) {
        if (element instanceof NumericTag nbtNumber) {
          return new ComparisonNbtPredicate(isNegated ? Comparator.NE : Comparator.EQ, nbtNumber);
        }
      }
      return new MatchPrimitiveNbtPredicate(element, isNegated);
    }
  }

  private @Nullable NbtPredicate parseFunctionGrammar(StringReader reader) throws CommandSyntaxException {
    final int cursorBeforeFunctionName = reader.getCursor();
    final NbtPredicate functionGrammar = NbtPredicateParsing.FUNCTIONS_PARSER.parse(parseContext);
    if (functionGrammar != null) {
      return functionGrammar;
    } else {
      reader.setCursor(cursorBeforeFunctionName);
    }
    return null;
  }
}
