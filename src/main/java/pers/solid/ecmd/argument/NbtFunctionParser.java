package pers.solid.ecmd.argument;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.function.nbt.*;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class NbtFunctionParser<S> {
  public static final Component MERGE = Component.translatable("enhanced_commands.nbt_function.merge");
  public static final Component EQUAL = Component.translatable("enhanced_commands.nbt_function.equal");
  public static final SimpleCommandExceptionType SIGN_EXPECTED = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_function.sign_expected"));
  public static final SimpleCommandExceptionType SIGN_UNEXPECTED_WHEN_REMOVING_KEY = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_function.sign_unexpected_when_removing_key"));
  public static final Component REMOVE_KEY = Component.translatable("enhanced_commands.nbt_function.remove_key");
  public static final Component ECLIPSE = Component.translatable("enhanced_commands.nbt_function.eclipse");
  public static final SimpleCommandExceptionType DUPLICATE_ECLIPSE = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_function.duplicate_eclipse"));
  public static final SimpleCommandExceptionType DUPLICATE_SEMICOLON = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_function.duplicate_semicolon"));
  public static final SimpleCommandExceptionType UNEXPECTED_SEMICOLON_AFTER_ECLIPSE = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_function.unexpected_semicolon_after_eclipse"));
  private final ParseContext<S> parseContext;

  public NbtFunctionParser(ParseContext<S> parseContext) {
    this.parseContext = parseContext;
  }

  private static CompletableFuture<Suggestions> suggestCompoundStart(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("{", NbtPredicateParser.START_OF_COMPOUND, suggestionsBuilder).buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestCompoundRemoveKey(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("-", REMOVE_KEY, suggestionsBuilder).buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestCompoundSeparate(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString(",", NbtPredicateParser.SEPARATE, suggestionsBuilder).buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestCompoundEnd(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("}", NbtPredicateParser.END_OF_COMPOUND, suggestionsBuilder).buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestListEnd(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("]", NbtPredicateParser.END_OF_LIST, suggestionsBuilder).buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestListSeparate(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString(",", NbtPredicateParser.SEPARATE, suggestionsBuilder).buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestListEclipse(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("...", ECLIPSE, suggestionsBuilder).buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestSign(SuggestionsBuilder suggestionsBuilder) {
    ParsingUtil.suggestString(":", MERGE, suggestionsBuilder);
    ParsingUtil.suggestString("=", EQUAL, suggestionsBuilder);
    return suggestionsBuilder.buildFuture();
  }

  static boolean parseColonOrEqual(boolean mustExpectSign, boolean equalsForDefault, StringReader reader, int cursorBeforeSign, SimpleCommandExceptionType signExpected) throws CommandSyntaxException {
    boolean isUsingEqual = equalsForDefault;
    if (!reader.canRead()) {
      reader.setCursor(cursorBeforeSign);
      throw signExpected.createWithContext(reader);
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
      throw signExpected.createWithContext(reader);
    }
    return isUsingEqual;
  }

  private static CompletableFuture<Suggestions> suggestValueDifferentTypes(SuggestionsBuilder suggestionsBuilder) {
    ParsingUtil.suggestString("{", NbtPredicateParser.START_OF_COMPOUND, suggestionsBuilder);
    ParsingUtil.suggestString("[", NbtPredicateParser.START_OF_LIST, suggestionsBuilder);
    return suggestionsBuilder.buildFuture();
  }

  public boolean parseSign(boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorBeforeSign = reader.getCursor();
    parseContext.addSuggestion((context, suggestionsBuilder) -> {
      final SuggestionsBuilder offset = suggestionsBuilder.createOffset(cursorBeforeSign);
      return suggestSign(offset);
    });
    if (!reader.canRead()) {
      if (mustExpectSign) {
        reader.setCursor(cursorBeforeSign);
        throw SIGN_EXPECTED.createWithContext(reader);
      } else {
        return equalsForDefault;
      }
    }

    parseContext.clearSuggestion();
    return parseColonOrEqual(mustExpectSign, equalsForDefault, reader, cursorBeforeSign, SIGN_EXPECTED);
  }

  public CompoundNbtFunction parseCompound(boolean isUsingEqual) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    parseContext.setSuggestion(NbtFunctionParser::suggestCompoundStart);
    reader.expect('{');
    parseContext.clearSuggestion();
    reader.skipWhitespace();
    Map<String, @Nullable NbtFunction> entries = new LinkedHashMap<>();

    while (!reader.canRead() || reader.peek() != '}') {
      reader.skipWhitespace();
      int cursorBeforeKey = reader.getCursor();
      parseContext.setSuggestion(NbtFunctionParser::suggestCompoundRemoveKey);
      final String key;
      boolean markAsRemoveKey = false;
      if (!reader.canRead()) {
        throw TagParser.ERROR_EXPECTED_KEY.createWithContext(reader);
      } else if (!isUsingEqual && reader.peek() == '-' && reader.canRead(2) && Character.isWhitespace(reader.peek(1))) {
        markAsRemoveKey = true;
        reader.skip();
        reader.skipWhitespace();
      }
      parseContext.clearSuggestion();
      key = reader.readString();
      if (key.isEmpty()) {
        reader.setCursor(cursorBeforeKey);
        throw TagParser.ERROR_EXPECTED_KEY.createWithContext(reader);
      }

      reader.skipWhitespace();
      if (!markAsRemoveKey) {
        entries.put(key, parseNbtFunction(true, false));
      } else {
        if (reader.canRead() && (reader.peek() == ':' || reader.peek() == '=')) {
          throw EnhancedCommandSyntaxException.withCursorEnd(SIGN_UNEXPECTED_WHEN_REMOVING_KEY.createWithContext(reader), reader.getCursor() + 1);
        }
        entries.put(key, null);
      }
      parseContext.terminateSuggestionsIfNotEmpty();
      parseContext.addSuggestion(NbtFunctionParser::suggestCompoundSeparate);
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
    parseContext.addSuggestion(NbtFunctionParser::suggestCompoundEnd);
    reader.expect('}');
    parseContext.clearSuggestion();
    return new CompoundNbtFunction(entries, !isUsingEqual);
  }

  /**
   * 解析列表。
   *
   * @see TagParser#readListTag()
   */
  public NbtFunction parseList(boolean isUsingEqual) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    reader.expect('[');
    reader.skipWhitespace();

    parseContext.setSuggestion(NbtFunctionParser::suggestListEnd);
    List<@Nullable Pair<@Nullable Integer, NbtFunction>> instructions = new ArrayList<>();

    boolean hasFoundEclipse = false;

    while (!reader.canRead() || reader.peek() != ']') {
      int cursorBeforeListElement = reader.getCursor();
      // 先检测是否有省略号。
      if (!hasFoundEclipse) {
        parseContext.addSuggestion((context, suggestionsBuilder) -> suggestListEclipse(context, suggestionsBuilder.createOffset(cursorBeforeListElement)));
      }
      if (reader.canRead(3) && reader.peek() == '.' && reader.peek(1) == '.' && reader.peek(2) == '.') {
        // 解析到了省略号的情况
        if (hasFoundEclipse) {
          throw EnhancedCommandSyntaxException.withCursorEnd(DUPLICATE_ECLIPSE.createWithContext(reader), reader.getCursor() + 3);
        }
        reader.setCursor(reader.getCursor() + 3);
        parseContext.clearSuggestion();
        hasFoundEclipse = true;

        instructions.add(null);

        // 解析省略号结束
      } else {
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
            parseContext.clearSuggestion();
            final NbtFunction nbtFunction = parseNbtFunction(true, false);
            parseContext.terminateSuggestionsIfNotEmpty();
            instructions.add(Pair.of(index, nbtFunction));
            isUsingPositionalPredicate = true;
          } catch (CommandSyntaxException e) {
            cursorWhenParsingPositionalFunction = reader.getCursor();
            exceptionWhenParsingPositionalFunction = e;
            suggestionsWhenParsingPositionalFunction = parseContext.getAllSuggestions();
            reader.setCursor(cursorBeforeListElement);
          }
        }
        if (!isUsingPositionalPredicate) {
          try {
            final NbtFunction nbtFunction = parseNbtFunction(false, isUsingEqual);
            parseContext.terminateSuggestionsIfNotEmpty();
            instructions.add(Pair.of(null, nbtFunction));
          } catch (CommandSyntaxException exception) {
            if (exceptionWhenParsingPositionalFunction != null) {
              reader.setCursor(cursorWhenParsingPositionalFunction);
              parseContext.replaceAllSuggestions(suggestionsWhenParsingPositionalFunction);
              throw exceptionWhenParsingPositionalFunction;
            } else {
              throw exception;
            }
          }
        }
      }

      reader.skipWhitespace();
      parseContext.terminateSuggestionsIfNotEmpty();
      parseContext.addSuggestion(NbtFunctionParser::suggestListSeparate);

      if (reader.canRead() && reader.peek() == ',') {
        reader.skip();
        parseContext.clearSuggestion();
        reader.skipWhitespace();
      } else {
        break;
      }
    } // end while

    reader.skipWhitespace();
    parseContext.addSuggestion(NbtFunctionParser::suggestListEnd);
    reader.expect(']');
    parseContext.clearSuggestion();

    // 解析完成，处理数据
    if (hasFoundEclipse) {
      final ImmutableList.Builder<NbtFunction> insertBefore = new ImmutableList.Builder<>();
      final ImmutableList.Builder<NbtFunction> insertAfter = new ImmutableList.Builder<>();
      ImmutableList.Builder<NbtFunction> currentAppending = insertBefore;

      for (Pair<@Nullable Integer, NbtFunction> instruction : instructions) {
        if (instruction == null) {
          currentAppending = insertAfter;
        } else {
          currentAppending.add(instruction.getSecond());
        }
      }

      return new ListInsertionNbtFunction(insertBefore.build(), insertAfter.build());
    } else {
      final ImmutableList.Builder<NbtFunction> replacements = new ImmutableList.Builder<>();
      final ImmutableList.Builder<PositionalListEntry<NbtFunction>> positional = new ImmutableList.Builder<>();

      for (Pair<@Nullable Integer, NbtFunction> instruction : instructions) {
        if (instruction == null) {
          // 这种情况一般不应该发生
        } else if (instruction.getFirst() == null) {
          replacements.add(instruction.getSecond());
        } else {
          positional.add(new PositionalListEntry<>(instruction.getFirst(), instruction.getSecond()));
        }
      }

      return new ListOpsNbtFunction(replacements.build(), positional.build());
    }
  }


  public NbtFunction parseNbtFunction(boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    // 尝试解析函数名称；如果不是函数语法，则恢复 cursor 重新解析；如果是函数语法，则按照函数语法解析，如果函数不存在，则报错。
    final StringReader reader = parseContext.reader();

    // 解析等号和不等号
    final boolean isUsingEqual = parseBeforeFunctionName(mustExpectSign, equalsForDefault, reader);

    final int cursorBeforeValue = reader.getCursor();
    parseContext.addSuggestion((context, suggestionsBuilder) -> suggestValueDifferentTypes(suggestionsBuilder.createOffset(cursorBeforeValue)));

    final NbtFunction functionGrammar = parseFunctionGrammar(reader);
    if (functionGrammar != null) return functionGrammar;

    if (!reader.canRead()) {
      throw TagParser.ERROR_EXPECTED_VALUE.createWithContext(reader);
    }
    if (reader.peek() == '{') {
      return parseCompound(isUsingEqual);
    } else if (reader.peek() == '[' && !(reader.canRead(3) && !StringReader.isQuotedStringStart(reader.peek()) && reader.peek(2) == ';')) {
      return parseList(isUsingEqual);
    } else {
      final Tag element = new TagParser(reader).readValue();

      if (isUsingEqual && element instanceof NumericTag abstractNbtNumber) {
        return new NumberValueNbtFunction(abstractNbtNumber);
      } else {
        return new SimpleNbtFunction(element);
      }
    }
  }

  /**
   * 解析复合标签或者函数语法的函数，不会解析其他类型如数字、列表等。与 {@link #parseCompound} 不同的是，仍会解析函数语法，尽管这些情况下的 NBT 函数运行的结果不一定是复合标签。
   */
  public NbtFunction parsePreferringCompound(boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();

    // 解析等号和不等号
    final boolean isUsingEqual = parseBeforeFunctionName(mustExpectSign, equalsForDefault, reader);

    final NbtFunction functionGrammar = parseFunctionGrammar(reader);
    if (functionGrammar != null) return functionGrammar;

    parseContext.terminateSuggestionsIfNotEmpty();
    parseContext.addSuggestion(NbtFunctionParser::suggestCompoundStart);
    return parseCompound(isUsingEqual);
  }

  private @Nullable NbtFunction parseFunctionGrammar(StringReader reader) throws CommandSyntaxException {
    final int cursorBeforeFunctionName = reader.getCursor();
    final NbtFunction functionGrammar = NbtFunctionParsing.FUNCTIONS_PARSER.parse(parseContext);
    if (functionGrammar != null) {
      return functionGrammar;
    } else {
      reader.setCursor(cursorBeforeFunctionName);
    }
    return null;
  }

  private boolean parseBeforeFunctionName(boolean mustExpectSign, boolean equalsForDefault, StringReader reader) throws CommandSyntaxException {
    final int cursorBeforeSign = reader.getCursor();
    final boolean isUsingEqual = parseSign(mustExpectSign, equalsForDefault);

    final boolean hasExplicitSign = cursorBeforeSign != reader.getCursor();
    reader.skipWhitespace();
    if (hasExplicitSign) {
      parseContext.clearSuggestion();
    }
    return isUsingEqual;
  }
}
