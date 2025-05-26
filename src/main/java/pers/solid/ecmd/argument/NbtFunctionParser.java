package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.function.nbt.*;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.parse.FunctionsParser;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NbtFunctionParser<S> {
  private final ParseContext<S> parseContext;

  public static final Text MERGE = Text.translatable("enhanced_commands.argument.nbt_function.merge");
  public static final Text EQUAL = Text.translatable("enhanced_commands.argument.nbt_function.equal");
  public static final Text SEMICOLON = Text.translatable("enhanced_commands.argument.nbt_function.semicolon");
  public static final SimpleCommandExceptionType SIGN_EXPECTED = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.nbt_function.sign_expected"));
  public static final SimpleCommandExceptionType SIGN_UNEXPECTED_WHEN_REMOVING_KEY = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.nbt_function.sign_unexpected_when_removing_key"));
  public static final Text REMOVE_KEY = Text.translatable("enhanced_commands.argument.nbt_function.remove_key");
  public static final Text ECLIPSE = Text.translatable("enhanced_commands.argument.nbt_function.eclipse");
  public static final SimpleCommandExceptionType DUPLICATE_ECLIPSE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.nbt_function.duplicate_eclipse"));
  public static final SimpleCommandExceptionType DUPLICATE_SEMICOLON = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.nbt_function.duplicate_semicolon"));
  public static final SimpleCommandExceptionType UNEXPECTED_SEMICOLON_AFTER_ECLIPSE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.nbt_function.unexpected_semicolon_after_eclipse"));

  public NbtFunctionParser(ParseContext<S> parseContext) {
    this.parseContext = parseContext;
  }

  public boolean parseSign(boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    boolean isUsingEqual = equalsForDefault;
    final SuggestedParser<S> parser = parseContext.parser();
    final StringReader reader = parser.reader;
    final int cursorBeforeSign = reader.getCursor();
    parser.setSuggestion((context, suggestionsBuilder) -> {
      ParsingUtil.suggestString(":", MERGE, suggestionsBuilder);
      ParsingUtil.suggestString("=", EQUAL, suggestionsBuilder);
      return suggestionsBuilder.buildFuture();
    });
    if (!reader.canRead()) {
      if (mustExpectSign) {
        reader.setCursor(cursorBeforeSign);
        throw SIGN_EXPECTED.createWithContext(reader);
      } else {
        return equalsForDefault;
      }
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
    return isUsingEqual;
  }

  public CompoundNbtFunction parseCompound(boolean isUsingEqual) throws CommandSyntaxException {
    final SuggestedParser<S> parser = parseContext.parser();
    final StringReader reader = parser.reader;
    parser.setSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("{", NbtPredicateParser.START_OF_COMPOUND, suggestionsBuilder).buildFuture());
    reader.expect('{');
    parser.clearSuggestion();
    reader.skipWhitespace();
    Map<String, NbtFunction> entries = new LinkedHashMap<>();

    while (!reader.canRead() || reader.peek() != '}') {
      reader.skipWhitespace();
      int cursorBeforeKey = reader.getCursor();
      parser.setSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("-", REMOVE_KEY, suggestionsBuilder).buildFuture());
      final String key;
      boolean markAsRemoveKey = false;
      if (!reader.canRead()) {
        throw StringNbtReader.EXPECTED_KEY.createWithContext(reader);
      } else if (!isUsingEqual && reader.peek() == '-' && reader.canRead(2) && Character.isWhitespace(reader.peek(1))) {
        markAsRemoveKey = true;
        reader.skip();
        reader.skipWhitespace();
      }
      parser.clearSuggestion();
      key = reader.readString();
      if (key != null && key.isEmpty()) {
        reader.setCursor(cursorBeforeKey);
        throw StringNbtReader.EXPECTED_KEY.createWithContext(reader);
      }

      reader.skipWhitespace();
      if (!markAsRemoveKey) {
        entries.put(key, parseFunction(true, false));
      } else {
        if (reader.canRead() && (reader.peek() == ':' || reader.peek() == '=')) {
          throw CommandSyntaxExceptionExtension.withCursorEnd(SIGN_UNEXPECTED_WHEN_REMOVING_KEY.createWithContext(reader), reader.getCursor() + 1);
        }
        entries.put(key, null);
      }
      parser.setSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString(",", NbtPredicateParser.SEPARATE, suggestionsBuilder).buildFuture());
      if (reader.canRead() && reader.peek() == ',') {
        reader.skip();
        parser.clearSuggestion();
        reader.skipWhitespace();
      } else {
        break;
      }
    }

    reader.skipWhitespace();
    parser.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("}", NbtPredicateParser.END_OF_COMPOUND, suggestionsBuilder).buildFuture());
    reader.expect('}');
    parser.clearSuggestion();
    return new CompoundNbtFunction(entries, !isUsingEqual);
  }


  /**
   * 解析列表。
   *
   * @see StringNbtReader#parseList()
   */
  public NbtFunction parseList(boolean isUsingEqual) throws CommandSyntaxException {
    final SuggestedParser<S> parser = parseContext.parser();
    final StringReader reader = parser.reader;
    reader.expect('[');
    reader.skipWhitespace();

    final SuggestionProvider<S> suggestEndOfList = (context, suggestionsBuilder) -> ParsingUtil.suggestString("]", NbtPredicateParser.END_OF_LIST, suggestionsBuilder).buildFuture();
    final SuggestionProvider<S> suggestSeparate = (context, suggestionsBuilder) -> ParsingUtil.suggestString(",", NbtPredicateParser.SEPARATE, suggestionsBuilder).buildFuture();
    final SuggestionProvider<S> suggestSemicolon = (context, suggestionsBuilder) -> ParsingUtil.suggestString(";", SEMICOLON, suggestionsBuilder).buildFuture();
    parser.setSuggestion(suggestEndOfList);
    if (reader.canRead() && reader.peek() == ']') {
      // 空列表
      reader.skip();
      parser.clearSuggestion();
      return new ListOpsNbtFunction(List.of(), Map.of(), Map.of());
    } else {
      // 列表根据分号，分为左边和右边两部分。
      // 左边的部分表示替换整个列表或者设置单个列表值，右边的部分则表示插值。
      // 如果没有分号，则根据是否有省略号来进行区分。
      List<Pair<@Nullable Integer, NbtFunction>> leftPartList = new ArrayList<>();
      List<Pair<@Nullable Integer, NbtFunction>> rightPartList = null;
      List<Pair<@Nullable Integer, NbtFunction>> currentlyAppendingList = leftPartList;

      boolean hasFoundEclipse = false;
      boolean hasFoundSemicolon = false;
      // 第一个元素后面可能会有分号。
      parser.addSuggestion(suggestSemicolon);
      if (reader.canRead() && reader.peek() == ';') {
        reader.skip();
        parser.clearSuggestion();
        hasFoundSemicolon = true;
        rightPartList = leftPartList;
        leftPartList = null;
      }

      while (!reader.canRead() || reader.peek() != ']') {
        int cursorBeforeListElement = reader.getCursor();
        // 先检测是否有省略号。
        if (!hasFoundEclipse) {
          parser.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("...", ECLIPSE, suggestionsBuilder).buildFuture());
        }
        if (reader.canRead(3) && reader.peek() == '.' && reader.peek(1) == '.' && reader.peek(2) == '.') {
          // 解析到了省略号的情况
          if (hasFoundEclipse) {
            throw CommandSyntaxExceptionExtension.withCursorEnd(DUPLICATE_ECLIPSE.createWithContext(reader), reader.getCursor() + 3);
          }
          reader.setCursor(reader.getCursor() + 3);
          parser.clearSuggestion();
          hasFoundEclipse = true;

          // 如果当前正在解析的是左边的部分，解析到省略号之后，说明当前正在解析的是右边的部分，需要进行迁移，同时之后不应再允许出现分号。
          if (currentlyAppendingList == leftPartList) {
            rightPartList = leftPartList;
            leftPartList = null;
          }
          rightPartList.add(null);

          // 解析完省略号之后，应该是逗号。
          reader.skipWhitespace();
          parser.addSuggestion(suggestSeparate);
          parser.addSuggestion(suggestEndOfList);
          if (!reader.canRead()) {
            reader.expect(',');
          } else if (reader.peek() == ']') {
            reader.skip(); // 结束列表
            parser.clearSuggestion();
            break;
          } else if (reader.peek() == ',') {
            reader.skip();
            reader.skipWhitespace();
            parser.setSuggestion(suggestEndOfList);
            if (!reader.canRead()) {
              throw StringNbtReader.EXPECTED_VALUE.createWithContext(reader);
            } else if (reader.peek() == ']') {
              reader.skip();
              parser.clearSuggestion();
              break;
            }
            continue;
          } else {
            parser.clearSuggestion();
            continue;
          }
        } // 解析省略号结束

        boolean isUsingPositionalPredicate = false;
        // 对于 isUsingEqual = false 的情况，尝试读取带有键的列表元素谓词
        // 例如：[2: "abc", 5 = "cde"]
        @Nullable CommandSyntaxException exceptionWhenParsingPositionalFunction = null;
        int cursorWhenParsingPositionalFunction = -1;
        @Unmodifiable List<SuggestionProvider<S>> suggestionsWhenParsingPositionalFunction = null;
        if (!isUsingEqual && reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
          try {
            final int index = reader.readInt();
            reader.skipWhitespace();
            parser.clearSuggestion();
            final NbtFunction nbtFunction = parseFunction(true, false);
            currentlyAppendingList.add(IntObjectPair.of(index, nbtFunction));
            isUsingPositionalPredicate = true;
          } catch (CommandSyntaxException e) {
            cursorWhenParsingPositionalFunction = reader.getCursor();
            exceptionWhenParsingPositionalFunction = e;
            suggestionsWhenParsingPositionalFunction = parser.getAllSuggestions();
            reader.setCursor(cursorBeforeListElement);
          }
        }
        if (!isUsingPositionalPredicate) {
          try {
            final NbtFunction nbtFunction = parseFunction(false, isUsingEqual);
            currentlyAppendingList.add(Pair.of(null, nbtFunction));
          } catch (CommandSyntaxException exception) {
            if (exceptionWhenParsingPositionalFunction != null) {
              reader.setCursor(cursorWhenParsingPositionalFunction);
              parser.replaceAllSuggestions(suggestionsWhenParsingPositionalFunction);
              throw exceptionWhenParsingPositionalFunction;
            } else {
              throw exception;
            }
          }
        }

        reader.skipWhitespace();
        parser.addSuggestion(suggestSeparate);
        if (currentlyAppendingList == leftPartList && !hasFoundSemicolon) {
          // 此时，可以有分号
          parser.addSuggestion(suggestSemicolon);
        }

        if (reader.canRead() && reader.peek() == ',') {
          reader.skip();
          reader.skipWhitespace();
          parser.setSuggestion(suggestEndOfList);
          if (!reader.canRead()) {
            throw StringNbtReader.EXPECTED_VALUE.createWithContext(reader);
          } else if (reader.peek() == ']') {
            reader.skip();
            parser.clearSuggestion();
            break;
          }
        } else if (reader.canRead() && reader.peek() == ';') {
          if (hasFoundSemicolon) {
            throw CommandSyntaxExceptionExtension.withCursorEnd(DUPLICATE_SEMICOLON.createWithContext(reader), reader.getCursor() + 1);
          } else if (currentlyAppendingList == rightPartList) {
            throw CommandSyntaxExceptionExtension.withCursorEnd(UNEXPECTED_SEMICOLON_AFTER_ECLIPSE.createWithContext(reader), reader.getCursor() + 1);
          } else {
            rightPartList = new ArrayList<>();
            currentlyAppendingList = rightPartList;
            parser.clearSuggestion();
            reader.skip();
            reader.skipWhitespace();
            hasFoundSemicolon = true;
          }
          parser.setSuggestion(suggestEndOfList);
          if (!reader.canRead()) {
            throw StringNbtReader.EXPECTED_VALUE.createWithContext(reader);
          } else if (reader.peek() == ']') {
            reader.skip();
            parser.clearSuggestion();
            break;
          }
        } else {
          parser.clearSuggestion();
          try {
            reader.skipWhitespace();
            parser.setSuggestion(suggestEndOfList);
            reader.expect(']'); // 结束列表
            break;
          } catch (CommandSyntaxException exception) {
            if (exceptionWhenParsingPositionalFunction != null) {
              reader.setCursor(cursorWhenParsingPositionalFunction);
              parser.replaceAllSuggestions(suggestionsWhenParsingPositionalFunction);
              throw exceptionWhenParsingPositionalFunction;
            } else {
              throw exception;
            }
          }
        } // end except ending square bracket
      } // end while

      parser.clearSuggestion();
      // 解析完成，处理数据
      final List<NbtFunction> valueReplacements = leftPartList == null ? null : leftPartList.stream().filter(pair -> !(pair instanceof IntObjectPair<NbtFunction>) && pair.left() == null).map(Pair::right).toList();
      final Int2ObjectMap<NbtFunction> positionalFunctions = leftPartList == null ? Int2ObjectMaps.emptyMap() : new Int2ObjectOpenHashMap<>();
      final Int2ObjectMap<List<NbtFunction>> positionalInsertions = rightPartList == null ? Int2ObjectMaps.emptyMap() : new Int2ObjectOpenHashMap<>();

      if (leftPartList != null) {
        for (Pair<Integer, NbtFunction> pair : leftPartList) {
          if (pair instanceof IntObjectPair<NbtFunction> intObjectPair) {
            positionalFunctions.put(intObjectPair.leftInt(), pair.right());
          }
        }
      }
      if (rightPartList != null) {
        int key = 0;
        List<NbtFunction> listToAppend = null;
        for (Pair<Integer, NbtFunction> pair : rightPartList) {
          if (pair == null) {
            // pair 为 null 时，说明遇到了省略号
            key = -1;
            listToAppend = null;
            continue;
          } else if (pair instanceof IntObjectPair<NbtFunction> intObjectPair) {
            key = intObjectPair.keyInt();
            listToAppend = new ArrayList<>();
            positionalInsertions.put(key, listToAppend);
          } else if (listToAppend == null) {
            listToAppend = new ArrayList<>();
            positionalInsertions.put(key, listToAppend);
          }
          listToAppend.add(pair.right());
        }
      }

      return new ListOpsNbtFunction(
          (valueReplacements == null || valueReplacements.isEmpty()) ? List.of() : valueReplacements,
          positionalFunctions,
          positionalInsertions
      );
    }
  }

  public NbtFunction parseFunction(boolean mustExpectSign, boolean equalsForDefault)
      throws CommandSyntaxException {
    // 尝试解析函数名称；如果不是函数语法，则恢复 cursor 重新解析；如果是函数语法，则按照函数语法解析，如果函数不存在，则报错。
    final SuggestedParser<S> parser = parseContext.parser();
    final StringReader reader = parser.reader;
    final int cursorBeforeFunctionName = reader.getCursor();
    final FunctionsParser<NbtFunctionArgument> functionsParser = new FunctionsParser<>(NbtFunctionTypes.FUNCTIONS, NbtFunctionTypes.FUNCTION_NAMES);
    // to be completed

    // 解析等号和不等号
    final int cursorBeforeSign = reader.getCursor();
    final boolean isUsingEqual = parseSign(mustExpectSign, equalsForDefault);
    final boolean hasExplicitSign = cursorBeforeSign != reader.getCursor();
    reader.skipWhitespace();
    if (hasExplicitSign) {
      parser.clearSuggestion();
    }
    parser.addSuggestion((context, suggestionsBuilder) -> {
      ParsingUtil.suggestString("{", NbtPredicateParser.START_OF_COMPOUND, suggestionsBuilder);
      ParsingUtil.suggestString("[", NbtPredicateParser.START_OF_LIST, suggestionsBuilder);
      return suggestionsBuilder.buildFuture();
    });
    if (!reader.canRead()) {
      throw StringNbtReader.EXPECTED_VALUE.createWithContext(reader);
    }
    if (reader.peek() == '{') {
      return parseCompound(isUsingEqual);
    } else if (reader.peek() == '[' && !(reader.canRead(3) && !StringReader.isQuotedStringStart(reader.peek()) && reader.peek(2) == ';')) {
      return parseList(isUsingEqual);
    } else {
      final NbtElement element = new StringNbtReader(reader).parseElement();
      parser.clearSuggestion();

      if (isUsingEqual && element instanceof AbstractNbtNumber abstractNbtNumber) {
        return new NumberValueNbtFunction(abstractNbtNumber);
      } else {
        return new SimpleNbtFunction(element);
      }
    }
  }
}
