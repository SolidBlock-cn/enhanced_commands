package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.function.nbt.ListOpsNbtFunction;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.function.nbt.SimpleNbtFunction;
import pers.solid.ecmd.util.SuggestionProvider;
import pers.solid.ecmd.util.SuggestionUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NbtFunctionSuggestedParser extends SuggestedParser {
  public NbtFunctionSuggestedParser(StringReader reader) {
    super(reader);
  }

  public NbtFunctionSuggestedParser(StringReader reader, List<SuggestionProvider> suggestions) {
    super(reader, suggestions);
  }

  public static final Text MERGE = Text.translatable("enhancedCommands.argument.nbt_function.merge");
  public static final Text EQUAL = Text.translatable("enhancedCommands.argument.nbt_function.equal");
  public static final Text SEMICOLON = Text.translatable("enhancedCommands.argument.nbt_function.semicolon");
  public static final SimpleCommandExceptionType SIGN_EXPECTED = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.nbt_function.sign_expected"));
  public static final SimpleCommandExceptionType SIGN_UNEXPECTED_WHEN_REMOVING_KEY = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.nbt_function.sign_unexpected_when_removing_key"));
  public static final Text REMOVE_KEY = Text.translatable("enhancedCommands.argument.nbt_function.remove_key");
  public static final Text ECLIPSE = Text.translatable("enhancedCommands.argument.nbt_function.eclipse");
  public static final SimpleCommandExceptionType DUPLICATE_ECLIPSE = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.nbt_function.duplicate_eclipse"));
  public static final SimpleCommandExceptionType DUPLICATE_SEMICOLON = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.nbt_function.duplicate_semicolon"));
  public static final SimpleCommandExceptionType UNEXPECTED_SEMICOLON_AFTER_ECLIPSE = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.nbt_function.unexpected_semicolon_after_eclipse"));

  public boolean parseSign(boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    boolean isUsingEqual = equalsForDefault;
    final int cursorBeforeSign = reader.getCursor();
    suggestions.add((commandContext, suggestionsBuilder) -> {
      SuggestionUtil.suggestString(":", MERGE, suggestionsBuilder);
      SuggestionUtil.suggestString("=", EQUAL, suggestionsBuilder);
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

  public NbtFunction parseCompound(boolean isUsingEqual) throws CommandSyntaxException {
    suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("{", NbtPredicateSuggestedParser.START_OF_COMPOUND, suggestionsBuilder));
    reader.expect('{');
    suggestions.clear();
    this.reader.skipWhitespace();
    Map<String, NbtFunction> entries = new LinkedHashMap<>();

    while (!this.reader.canRead() || this.reader.peek() != '}') {
      reader.skipWhitespace();
      int cursorBeforeKey = this.reader.getCursor();
      suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("-", REMOVE_KEY, suggestionsBuilder));
      final String key;
      boolean markAsRemoveKey = false;
      if (!this.reader.canRead()) {
        throw StringNbtReader.EXPECTED_KEY.createWithContext(this.reader);
      } else if (!isUsingEqual && reader.peek() == '-') {
        markAsRemoveKey = true;
        reader.skip();
        reader.skipWhitespace();
      }
      key = this.reader.readString();
      if (key != null && key.isEmpty()) {
        this.reader.setCursor(cursorBeforeKey);
        throw StringNbtReader.EXPECTED_KEY.createWithContext(this.reader);
      }

      reader.skipWhitespace();
      if (!markAsRemoveKey) {
        entries.put(key, parseFunction(true, false));
      } else {
        if (reader.canRead() && (reader.peek() == ':' || reader.peek() == '=')) {
          throw SIGN_UNEXPECTED_WHEN_REMOVING_KEY.createWithContext(reader);
        }
        entries.put(key, null);
      }
      suggestions.clear();
      suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString(",", NbtPredicateSuggestedParser.SEPARATE, suggestionsBuilder));
      if (reader.canRead() && reader.peek() == ',') {
        reader.skip();
        suggestions.clear();
        reader.skipWhitespace();
      } else {
        break;
      }
    }

    reader.skipWhitespace();
    suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("}", NbtPredicateSuggestedParser.END_OF_COMPOUND, suggestionsBuilder));
    reader.expect('}');
    suggestions.clear();
    return new CompoundNbtFunction(entries, !isUsingEqual);
  }


  /**
   * 解析列表。
   *
   * @see StringNbtReader#parseList()
   */
  public NbtFunction parseList(boolean isUsingEqual) throws CommandSyntaxException {
    reader.expect('[');
    suggestions.clear();
    this.reader.skipWhitespace();

    suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("]", NbtPredicateSuggestedParser.END_OF_LIST, suggestionsBuilder));
    if (reader.canRead() && reader.peek() == ']') {
      // 空列表
      reader.skip();
      suggestions.clear();
      return new ListOpsNbtFunction(List.of(), null, null);
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
      suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString(";", SEMICOLON, suggestionsBuilder));
      if (reader.canRead() && reader.peek() == ';') {
        reader.skip();
        suggestions.clear();
        hasFoundSemicolon = true;
        rightPartList = leftPartList;
        leftPartList = null;
      }

      while (!this.reader.canRead() || this.reader.peek() != ']') {
        int cursorBeforeListElement = this.reader.getCursor();
        // 先检测是否有省略号。
        if (!hasFoundEclipse) {
          suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("...", ECLIPSE, suggestionsBuilder));
        }
        if (reader.canRead(3) && reader.peek() == '.' && reader.peek(1) == '.' && reader.peek(2) == '.') {
          // 解析到了省略号的情况
          if (hasFoundEclipse) {
            throw DUPLICATE_ECLIPSE.createWithContext(reader);
          }
          reader.setCursor(reader.getCursor() + 3);
          suggestions.clear();
          hasFoundEclipse = true;

          // 如果当前正在解析的是左边的部分，解析到省略号之后，说明当前正在解析的是右边的部分，需要进行迁移，同时之后不应再允许出现分号。
          if (currentlyAppendingList == leftPartList) {
            rightPartList = leftPartList;
            leftPartList = null;
          }
          rightPartList.add(null);

          // 解析完省略号之后，应该是逗号。
          reader.skipWhitespace();
          suggestions.add((context, suggestionsBuilder) -> {
            SuggestionUtil.suggestString(",", NbtPredicateSuggestedParser.SEPARATE, suggestionsBuilder);
            SuggestionUtil.suggestString("]", NbtPredicateSuggestedParser.END_OF_LIST, suggestionsBuilder);
          });
          if (!reader.canRead()) {
            reader.expect(',');
          } else if (reader.peek() == ']') {
            reader.skip(); // 结束列表
            suggestions.clear();
            break;
          } else if (reader.peek() == ',') {
            reader.skip();
            suggestions.clear();
            continue;
          } else {
            suggestions.clear();
            continue;
          }
        }

        boolean isUsingPositionalPredicate = false;
        // 对于 isUsingEqual = false 的情况，尝试读取带有键的列表元素谓词
        // 例如：[2: "abc", 5 = "cde"]
        @Nullable CommandSyntaxException exceptionWhenParsingPositionalFunction = null;
        int cursorWhenParsingPositionalFunction = -1;
        @Nullable List<SuggestionProvider> suggestionsWhenParsingPositionalFunction = null;
        if (!isUsingEqual && reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
          try {
            final int index = reader.readInt();
            reader.skipWhitespace();
            final NbtFunction nbtFunction = parseFunction(true, false);
            currentlyAppendingList.add(IntObjectPair.of(index, nbtFunction));
            isUsingPositionalPredicate = true;
          } catch (CommandSyntaxException e) {
            cursorWhenParsingPositionalFunction = reader.getCursor();
            exceptionWhenParsingPositionalFunction = e;
            suggestionsWhenParsingPositionalFunction = List.copyOf(suggestions);
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
              suggestions.addAll(suggestionsWhenParsingPositionalFunction);
              throw exceptionWhenParsingPositionalFunction;
            } else {
              throw exception;
            }
          }
        }

        this.reader.skipWhitespace();
        suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString(",", NbtPredicateSuggestedParser.SEPARATE, suggestionsBuilder));
        if (currentlyAppendingList == leftPartList && !hasFoundSemicolon) {
          // 此时，可以有分号
          suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString(";", SEMICOLON, suggestionsBuilder));
        }

        if (this.reader.canRead() && this.reader.peek() == ',') {
          this.reader.skip();
          suggestions.clear();
          this.reader.skipWhitespace();
        } else if (this.reader.canRead() && this.reader.peek() == ';') {
          if (hasFoundSemicolon) {
            throw DUPLICATE_SEMICOLON.createWithContext(reader);
          } else if (currentlyAppendingList == rightPartList) {
            throw UNEXPECTED_SEMICOLON_AFTER_ECLIPSE.createWithContext(reader);
          } else {
            rightPartList = new ArrayList<>();
            currentlyAppendingList = rightPartList;
            suggestions.clear();
            reader.skip();
            reader.skipWhitespace();
            hasFoundSemicolon = true;
          }
        } else {
          suggestions.clear();
          try {
            reader.skipWhitespace();
            suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("]", NbtPredicateSuggestedParser.END_OF_LIST, suggestionsBuilder));
            reader.expect(']'); // 结束列表
            break;
          } catch (CommandSyntaxException exception) {
            if (exceptionWhenParsingPositionalFunction != null) {
              reader.setCursor(cursorWhenParsingPositionalFunction);
              suggestions.clear();
              suggestions.addAll(suggestionsWhenParsingPositionalFunction);
              throw exceptionWhenParsingPositionalFunction;
            } else {
              throw exception;
            }
          }
        } // end expect ending square bracket
      } // end while

      suggestions.clear();
      // 解析完成，处理数据
      final List<NbtFunction> valueReplacements = leftPartList == null ? null : leftPartList.stream().filter(pair -> !(pair instanceof IntObjectPair<NbtFunction>) && pair.left() == null).map(Pair::right).toList();
      final Int2ObjectMap<NbtFunction> positionalFunctions = leftPartList == null ? null : new Int2ObjectOpenHashMap<>();
      final Int2ObjectMap<List<NbtFunction>> positionalInsertions = rightPartList == null ? null : new Int2ObjectOpenHashMap<>();

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
          (valueReplacements == null || valueReplacements.isEmpty()) ? null : valueReplacements,
          positionalFunctions,
          positionalInsertions
      );
    }
  }

  public NbtFunction parseFunction(boolean mustExpectSign, boolean equalsForDefault)
      throws CommandSyntaxException {

    // 解析等号和不等号
    final int cursorBeforeSign = reader.getCursor();
    final boolean isUsingEqual = parseSign(mustExpectSign, equalsForDefault);
    final boolean hasExplicitSign = cursorBeforeSign != reader.getCursor();
    reader.skipWhitespace();
    if (hasExplicitSign) {
      suggestions.clear();
    }
    suggestions.add((context, suggestionsBuilder) -> {
      SuggestionUtil.suggestString("{", NbtPredicateSuggestedParser.START_OF_COMPOUND, suggestionsBuilder);
      SuggestionUtil.suggestString("[", NbtPredicateSuggestedParser.START_OF_LIST, suggestionsBuilder);
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
      suggestions.clear();
      return new SimpleNbtFunction(element);
    }
  }
}
