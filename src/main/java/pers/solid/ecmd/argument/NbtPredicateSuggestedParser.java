package pers.solid.ecmd.argument;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.*;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.predicate.nbt.*;
import pers.solid.ecmd.predicate.property.Comparator;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.SuggestionProvider;
import pers.solid.ecmd.util.SuggestionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class NbtPredicateSuggestedParser extends SuggestedParser {
  public static final Text MATCH = Text.translatable("enhancedCommands.argument.nbt_predicate.tooltip.match");
  public static final Text EQUAL = Text.translatable("enhancedCommands.argument.nbt_predicate.tooltip.equal");
  public static final Text NOT_MATCH = Text.translatable("enhancedCommands.argument.nbt_predicate.tooltip.not_match");
  public static final Text NOT_EQUAL = Text.translatable("enhancedCommands.argument.nbt_predicate.tooltip.not_equal");
  public static final Text REGEX = Text.translatable("enhancedCommands.argument.nbt_predicate.tooltip.regex");
  public static final Text NOT_REGEX = Text.translatable("enhancedCommands.argument.nbt_predicate.tooltip.not_regex");
  public static final Text ANY_KEY = Text.translatable("enhancedCommands.argument.nbt_predicate.tooltip.any_key");
  public static final Text ANY_VALUE = Text.translatable("enhancedCommands.argument.nbt_predicate.tooltip.any_value");
  public static final Text SEPARATE = Text.translatable("enhancedCommands.argument.nbt_predicate.tooltip.separate");
  public static final Text START_OF_COMPOUND = Text.translatable("enhancedCommands.argument.nbt_predicate.tooltip.start_of_compound");
  public static final Text END_OF_COMPOUND = Text.translatable("enhancedCommands.argument.nbt_predicate.tooltip.end_of_compound");
  public static final Text START_OF_LIST = Text.translatable("enhancedCommands.argument.nbt_predicate.tooltip.start_of_list");
  public static final Text END_OF_LIST = Text.translatable("enhancedCommands.argument.nbt_predicate.tooltip.end_of_list");

  public static final SimpleCommandExceptionType SIGN_EXPECTED = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.nbt_predicate.sign_expected"));
  public static final DynamicCommandExceptionType DUPLICATE_KEY = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.nbt_predicate.duplicate_key", o));
  public static final DynamicCommandExceptionType MUST_BE_NUMBER_OR_STRING = new DynamicCommandExceptionType(actualType -> Text.translatable("enhancedCommands.argument.nbt_predicate.must_be_number_or_string", actualType));

  public NbtPredicateSuggestedParser(StringReader reader) {
    super(reader);
  }

  public NbtPredicateSuggestedParser(StringReader reader, List<SuggestionProvider> suggestions) {
    super(reader, suggestions);
  }

  /**
   * <p>解析符号，并提供建议。这个符号可以是 {@code ":"}、{@code "="}。符号前面可以加个 {@code "!"} 以表示否定。
   * <p>在部分情况下，这个符号是可选的。例如，在列表中，每个元素的谓词可以不包含符号。但是，在复合标签中，在解析完键后，就必须要包含符号。
   *
   * @param mustExpectSign   是否必须要以 {@code ":"} 或者 {@code "="} 符号作为开头。
   * @param equalsForDefault 在没有符号作为前缀时，是否默认为 {@code "="}，而不是 {@code ":"}。
   */
  public byte parseSign(boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    boolean isUsingEqual = equalsForDefault;
    boolean isNegated = false;
    final int cursorBeforeSign = reader.getCursor();
    suggestions.add((commandContext, suggestionsBuilder) -> {
      SuggestionUtil.suggestString(":", MATCH, suggestionsBuilder);
      SuggestionUtil.suggestString("!:", NOT_MATCH, suggestionsBuilder);
      SuggestionUtil.suggestString("=", EQUAL, suggestionsBuilder);
      SuggestionUtil.suggestString("!=", NOT_EQUAL, suggestionsBuilder);
    });
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
    if (!reader.canRead()) {
      reader.setCursor(cursorBeforeSign);
      throw SIGN_EXPECTED.createWithContext(reader);
    }
    if (reader.peek() == ':') {
      isUsingEqual = false;
      reader.skip();
      reader.skipWhitespace();
    } else if (reader.peek() == '=') {
      isUsingEqual = true;
      reader.skip();
      reader.skipWhitespace();
    } else if (mustExpectSign) {
      reader.setCursor(cursorBeforeSign);
      throw SIGN_EXPECTED.createWithContext(reader);
    }
    return (byte) ((isUsingEqual ? 2 : 0) + (isNegated ? 1 : 0));
  }

  /**
   * 解析复合标签。
   *
   * @see StringNbtReader#parseCompound()
   */
  public NbtPredicate parseCompound(boolean isUsingEqual, boolean isNegated) throws CommandSyntaxException {
    suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("{", START_OF_COMPOUND, suggestionsBuilder));
    reader.expect('{');
    suggestions.clear();
    this.reader.skipWhitespace();
    ListMultimap<String, NbtPredicate> entries = LinkedListMultimap.create();

    while (!this.reader.canRead() || this.reader.peek() != '}') {
      reader.skipWhitespace();
      int cursorBeforeKey = this.reader.getCursor();
      suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("*", ANY_KEY, suggestionsBuilder));
      final String key;
      if (!this.reader.canRead()) {
        throw StringNbtReader.EXPECTED_KEY.createWithContext(this.reader);
      } else if (!isUsingEqual && reader.peek() == '*') {
        key = null;
        reader.skip();
      } else {
        key = this.reader.readString();
      }
      if (key != null && key.isEmpty()) {
        this.reader.setCursor(cursorBeforeKey);
        throw StringNbtReader.EXPECTED_KEY.createWithContext(this.reader);
      }

      reader.skipWhitespace();
      entries.put(key, parsePredicate(true, false));
      suggestions.clear();
      suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString(",", SEPARATE, suggestionsBuilder));
      if (reader.canRead() && reader.peek() == ',') {
        reader.skip();
        suggestions.clear();
        reader.skipWhitespace();
      } else {
        break;
      }
    }

    reader.skipWhitespace();
    suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("}", END_OF_COMPOUND, suggestionsBuilder));
    reader.expect('}');
    suggestions.clear();
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
   * @see StringNbtReader#parseList()
   */
  public NbtPredicate parseList(boolean isUsingEqual, boolean isNegated) throws CommandSyntaxException {
    reader.expect('[');
    suggestions.clear();
    this.reader.skipWhitespace();
    final List<@NotNull NbtPredicate> expected = new ArrayList<>();
    final List<IntObjectPair<NbtPredicate>> expectedPositional = isUsingEqual ? null : new ArrayList<>();

    suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("]", END_OF_LIST, suggestionsBuilder));
    if (reader.canRead() && reader.peek() == ']') {
      // 空列表
      reader.skip();
      suggestions.clear();
    } else {
      while (!this.reader.canRead() || this.reader.peek() != ']') {
        int cursorBeforeListElement = this.reader.getCursor();
        boolean isUsingPositionalPredicate = false;
        // 对于 isUsingEqual = false 的情况，尝试读取带有键的列表元素谓词
        // 例如：[2: "abc", 5 = "cde"]
        @Nullable CommandSyntaxException exceptionWhenParsingPositionalPredicate = null;
        int cursorWhenParsingPositionalPredicate = -1;
        @Nullable List<SuggestionProvider> suggestionsWhenParsingPositionalPredicate = null;
        if (!isUsingEqual && reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
          try {
            final int index = reader.readInt();
            reader.skipWhitespace();
            final NbtPredicate nbtPredicate = parsePredicate(true, false);
            expectedPositional.add(IntObjectPair.of(index, nbtPredicate));
            isUsingPositionalPredicate = true;
          } catch (CommandSyntaxException e) {
            cursorWhenParsingPositionalPredicate = reader.getCursor();
            exceptionWhenParsingPositionalPredicate = e;
            suggestionsWhenParsingPositionalPredicate = List.copyOf(suggestions);
            reader.setCursor(cursorBeforeListElement);
          }
        }
        if (!isUsingPositionalPredicate) {
          try {
            final NbtPredicate nbtPredicate = parsePredicate(false, isUsingEqual);
            expected.add(nbtPredicate);
          } catch (CommandSyntaxException exception) {
            if (exceptionWhenParsingPositionalPredicate != null) {
              reader.setCursor(cursorWhenParsingPositionalPredicate);
              suggestions.addAll(suggestionsWhenParsingPositionalPredicate);
              throw exceptionWhenParsingPositionalPredicate;
            } else {
              throw exception;
            }
          }
        }

        this.reader.skipWhitespace();
        suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString(",", SEPARATE, suggestionsBuilder));
        if (this.reader.canRead() && this.reader.peek() == ',') {
          this.reader.skip();
          suggestions.clear();
          this.reader.skipWhitespace();
        } else {
          suggestions.clear();
          try {
            reader.skipWhitespace();
            suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("]", END_OF_LIST, suggestionsBuilder));
            reader.expect(']'); // 结束列表
          } catch (CommandSyntaxException exception) {
            if (exceptionWhenParsingPositionalPredicate != null) {
              reader.setCursor(cursorWhenParsingPositionalPredicate);
              suggestions.clear();
              suggestions.addAll(suggestionsWhenParsingPositionalPredicate);
              throw exceptionWhenParsingPositionalPredicate;
            } else {
              throw exception;
            }
          }
          break;
        }
      }
    }

    suggestions.clear();
    return isUsingEqual ? new EqualsListNbtPredicate(expected, isNegated) : new MatchListNbtPredicate(expected, expectedPositional, isNegated);
  }

  public NbtPredicate parsePredicate(boolean mustExpectSign, boolean equalsForDefault)
      throws CommandSyntaxException {
    // 尝试读取正则表达式语法
    suggestions.add((context, suggestionsBuilder) -> {
      SuggestionUtil.suggestString("~", REGEX, suggestionsBuilder);
      SuggestionUtil.suggestString("!~", NOT_REGEX, suggestionsBuilder);
    });
    if ((reader.canRead() && reader.peek() == '~') || (reader.canRead(2) && reader.peek() == '!' && reader.peek(1) == '~')) {
      final boolean isNegated = reader.peek() == '!';
      reader.skip();
      if (isNegated) {
        reader.skip();
      }
      // 开始解析字符串，并将其视为正则表达式
      suggestions.clear();
      reader.skipWhitespace();
      final int cursorBeforeRegex = reader.getCursor();
      try {
        return new RegexNbtPredicate(Pattern.compile(reader.readString()), false);
      } catch (PatternSyntaxException e) {
        reader.setCursor(cursorBeforeRegex + e.getIndex());
        throw ModCommandExceptionTypes.INVALID_REGEX.createWithContext(reader, e.getDescription());
      }
    }

    // 尝试读取比较值（除了等号和不等号之外的值）
    suggestions.add((context, suggestionsBuilder) -> CommandSource.suggestMatching(Arrays.stream(Comparator.values()).filter(Predicates.not(Predicates.in(List.of(Comparator.EQ, Comparator.NE)))).map(Comparator::asString), suggestionsBuilder));

    final int cursorBeforeSign = reader.getCursor();
    for (Comparator comparator : Comparator.values()) {
      if (comparator != Comparator.EQ && comparator != Comparator.NE) {
        final String name = comparator.asString();
        if (reader.getRemaining().startsWith(name)) {
          // 防止这种情况：“<=”开头的被解析为“>”
          if (comparator == Comparator.LT || comparator == Comparator.GT) {
            if (reader.canRead(2) && reader.peek(1) == '=') {
              continue;
            }
          }

          // 解析完成了符号，后面应该是数字或者字符串
          reader.setCursor(reader.getCursor() + name.length());
          suggestions.clear();
          reader.skipWhitespace();

          // 尝试读取一个数字或字符串
          if (!reader.canRead()) {
            throw StringNbtReader.EXPECTED_VALUE.createWithContext(reader);
          } else if (reader.peek() == '{') {
            throw MUST_BE_NUMBER_OR_STRING.createWithContext(reader, NbtCompound.TYPE.getCommandFeedbackName());
          } else if (reader.peek() == '[') {
            throw MUST_BE_NUMBER_OR_STRING.createWithContext(reader, NbtList.TYPE.getCommandFeedbackName());
          }
          final NbtElement element = new StringNbtReader(reader).parseElement();
          if (element instanceof NbtString || element instanceof AbstractNbtNumber) {
            return new ComparisonNbtPredicate(comparator, element);
          } else {
            throw MUST_BE_NUMBER_OR_STRING.createWithContext(reader, element.getNbtType().getCommandFeedbackName());
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
      suggestions.clear();
    }
    suggestions.add((context, suggestionsBuilder) -> {
      SuggestionUtil.suggestString("*", ANY_VALUE, suggestionsBuilder);
      SuggestionUtil.suggestString("{", START_OF_COMPOUND, suggestionsBuilder);
      SuggestionUtil.suggestString("[", START_OF_LIST, suggestionsBuilder);
    });
    if (!reader.canRead()) {
      throw StringNbtReader.EXPECTED_VALUE.createWithContext(reader);
    }
    if (reader.peek() == '*') {
      reader.skip();
      suggestions.clear();
      return ConstantNbtPredicate.of(!isNegated);
    } else if (reader.peek() == '{') {
      return parseCompound(isUsingEqual, isNegated);
    } else if (reader.peek() == '[' && !(reader.canRead(3) && !StringReader.isQuotedStringStart(reader.peek()) && reader.peek(2) == ';')) {
      return parseList(isUsingEqual, isNegated);
    } else {
      // 先尝试读取 NumberRange
      final int cursorBeforeRange = reader.getCursor();
      try {
        final NumberRange.FloatRange parsedRange = NumberRange.FloatRange.parse(reader);
        if (parsedRange.getMin() != null && !parsedRange.getMin().equals(parsedRange.getMax())) {
          return new RangeNbtPredicate(parsedRange, isNegated);
        } else {
          reader.setCursor(cursorBeforeRange);
        }
      } catch (CommandSyntaxException ignored) {
      }

      final NbtElement element = new StringNbtReader(reader).parseElement();
      if (isUsingEqual) {
        if (element instanceof AbstractNbtNumber nbtNumber) {
          return new ComparisonNbtPredicate(isNegated ? Comparator.NE : Comparator.EQ, nbtNumber);
        }
      }
      suggestions.clear();
      return new MatchPrimitiveNbtPredicate(element, isNegated);
    }
  }
}
