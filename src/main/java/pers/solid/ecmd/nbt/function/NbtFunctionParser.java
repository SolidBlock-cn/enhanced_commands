package pers.solid.ecmd.nbt.function;

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
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.nbt.NbtParserShared;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class NbtFunctionParser {
  public static final Component MERGE = Component.translatable("enhanced_commands.nbt_function.merge");
  public static final Component EQUAL = Component.translatable("enhanced_commands.nbt_function.equal");
  public static final SimpleCommandExceptionType SIGN_EXPECTED = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_function.sign_expected"));
  public static final SimpleCommandExceptionType SIGN_UNEXPECTED_WHEN_REMOVING_KEY = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_function.sign_unexpected_when_removing_key"));
  public static final Component REMOVE_KEY = Component.translatable("enhanced_commands.nbt_function.remove_key");
  public static final Component ECLIPSE = Component.translatable("enhanced_commands.nbt_function.eclipse");
  public static final SimpleCommandExceptionType DUPLICATE_ECLIPSE = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_function.duplicate_eclipse"));
  public static final Component OVERLAY_TOOLTIP = Component.translatable("enhanced_commands.function.overlay.symbol_tooltip");
  public static final Component PICK_TOOLTIP = Component.translatable("enhanced_commands.function.pick.symbol_tooltip");

  private NbtFunctionParser() {
  }

  private static CompletableFuture<Suggestions> suggestCompoundRemoveKey(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("-", REMOVE_KEY, suggestionsBuilder).buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestListEclipse(SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("...", ECLIPSE, suggestionsBuilder).buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestSign(SuggestionsBuilder suggestionsBuilder) {
    ParsingUtil.suggestString(":", MERGE, suggestionsBuilder);
    ParsingUtil.suggestString("=", EQUAL, suggestionsBuilder);
    return suggestionsBuilder.buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestValueDifferentTypes(SuggestionsBuilder suggestionsBuilder) {
    ParsingUtil.suggestString("{", NbtParserShared.START_OF_COMPOUND, suggestionsBuilder);
    ParsingUtil.suggestString("[", NbtParserShared.START_OF_LIST, suggestionsBuilder);
    return suggestionsBuilder.buildFuture();
  }

  public static <S> boolean parseSign(ParseContext<S> parseContext, boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
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
    return NbtParserShared.parseColonOrEqual(mustExpectSign, equalsForDefault, reader, cursorBeforeSign, SIGN_EXPECTED);
  }

  public static <S> CompoundNbtFunction parseCompound(ParseContext<S> parseContext, boolean isUsingEqual) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    NbtParserShared.handleCompoundStart(parseContext, reader);

    Map<String, Optional<NbtFunction>> entries = new LinkedHashMap<>();

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
        entries.put(key, Optional.of(NbtFunctionParser.parseNbtFunction(parseContext, true, false)));
      } else {
        if (reader.canRead() && (reader.peek() == ':' || reader.peek() == '=')) {
          throw EnhancedCommandSyntaxException.withCursorEnd(SIGN_UNEXPECTED_WHEN_REMOVING_KEY.createWithContext(reader), reader.getCursor() + 1);
        }
        entries.put(key, Optional.empty());
      }

      if (NbtParserShared.handleCompoundSeparate(parseContext, reader)) {
        break;
      }
    }

    NbtParserShared.handleCompoundEnd(parseContext, reader);
    return new CompoundNbtFunction(entries, !isUsingEqual);
  }

  /**
   * 解析列表。
   *
   * @see TagParser#readListTag()
   */
  public static <S> NbtFunction parseList(ParseContext<S> parseContext, boolean isUsingEqual) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    NbtParserShared.handListStart(parseContext, reader);

    List<@Nullable Pair<@Nullable Integer, NbtFunction>> instructions = new ArrayList<>();
    boolean hasFoundEclipse = false;
    while (!reader.canRead() || reader.peek() != ']') {
      int cursorBeforeListElement = reader.getCursor();
      // 先检测是否有省略号。
      if (!hasFoundEclipse) {
        parseContext.addSuggestion((context, suggestionsBuilder) -> suggestListEclipse(suggestionsBuilder.createOffset(cursorBeforeListElement)));
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
            final NbtFunction nbtFunction = NbtFunctionParser.parseNbtFunction(parseContext, true, false);
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
            final NbtFunction nbtFunction = NbtFunctionParser.parseNbtFunction(parseContext, false, isUsingEqual);
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
      if (NbtParserShared.handleListSeparate(parseContext, reader)) {
        break;
      }
    } // end while

    NbtParserShared.handleListEnd(parseContext, reader);

    // 解析完成，处理数据
    if (hasFoundEclipse) {
      final ImmutableList.Builder<NbtFunction> insertBefore = new ImmutableList.Builder<>();
      final ImmutableList.Builder<NbtFunction> insertAfter = new ImmutableList.Builder<>();
      final ImmutableList.Builder<PositionalListEntry<NbtFunction>> insertPositional = new ImmutableList.Builder<>();
      ImmutableList.Builder<NbtFunction> currentAppending = insertBefore;

      for (Pair<@Nullable Integer, NbtFunction> instruction : instructions) {
        if (instruction == null) {
          currentAppending = insertAfter;
        } else if (instruction.getFirst() != null) {
          insertPositional.add(new PositionalListEntry<>(instruction.getFirst(), instruction.getSecond()));
        } else {
          currentAppending.add(instruction.getSecond());
        }
      }

      return new ListInsertionNbtFunction(insertBefore.build(), insertAfter.build(), insertPositional.build());
    } else {
      final ImmutableList.Builder<NbtFunction> replacements = new ImmutableList.Builder<>();
      final ImmutableList.Builder<PositionalListEntry<NbtFunction>> positional = new ImmutableList.Builder<>();

      for (Pair<@Nullable Integer, NbtFunction> instruction : instructions) {
        if (instruction == null) {
          continue;
        }
        if (instruction.getFirst() == null) {
          replacements.add(instruction.getSecond());
        } else {
          positional.add(new PositionalListEntry<>(instruction.getFirst(), instruction.getSecond()));
        }
      }

      return new ListOpsNbtFunction(replacements.build(), positional.build());
    }
  }

  public static <S> NbtFunction parseNbtFunction(ParseContext<S> parseContext, boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    // 尝试解析函数名称；如果不是函数语法，则恢复 cursor 重新解析；如果是函数语法，则按照函数语法解析，如果函数不存在，则报错。

    // 解析等号和不等号
    final boolean isUsingEqual = NbtFunctionParser.parseBeforeFunctionName(parseContext, mustExpectSign, equalsForDefault);

    return parseAfterSign(parseContext, isUsingEqual);
  }

  private static <S> NbtFunction parseAfterSign(ParseContext<S> parseContext, boolean isUsingEqual) throws CommandSyntaxException {
    return parsePick(parseContext, isUsingEqual);
  }

  static NbtFunction parsePick(ParseContext<?> parseContext, boolean isUsingEqual) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseOverlay(parseContext, isUsingEqual), functions -> {
      ImmutableList.Builder<NbtFunction> builder = new ImmutableList.Builder<>();
      for (NbtFunction function : functions) {
        builder.add(function);
      }
      return new PickNbtFunction(new WeightedList.Uniform<>(builder.build()));
    }, "|", PICK_TOOLTIP, parseContext);
  }

  static NbtFunction parseOverlay(ParseContext<?> parseContext, boolean isUsingEqual) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseUnit(parseContext, isUsingEqual), functions -> {
      ImmutableList.Builder<NbtFunction> builder = new ImmutableList.Builder<>();
      for (NbtFunction blockFunction : functions) {
        builder.add(blockFunction);
      }
      return new OverlayNbtFunction(builder.build());
    }, "*", OVERLAY_TOOLTIP, parseContext);
  }

  private static <S> NbtFunction parseUnit(ParseContext<S> parseContext, boolean isUsingEqual) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorBeforeValue = reader.getCursor();
    parseContext.addSuggestion((context, suggestionsBuilder) -> suggestValueDifferentTypes(suggestionsBuilder.createOffset(cursorBeforeValue)));

    final NbtFunction functionGrammar = NbtFunctionParser.parseFunctionGrammar(parseContext);
    if (functionGrammar != null) return functionGrammar;

    final ReferenceNbtFunction reference = ReferenceNbtFunction.PREFIXED_ID_PARSER.parse(parseContext);
    if (reference != null) return reference;

    if (!reader.canRead()) {
      throw TagParser.ERROR_EXPECTED_VALUE.createWithContext(reader);
    }
    if (reader.peek() == '{') {
      return parseCompound(parseContext, isUsingEqual);
    } else if (reader.peek() == '[' && !(reader.canRead(3) && !StringReader.isQuotedStringStart(reader.peek()) && reader.peek(2) == ';')) {
      return parseList(parseContext, isUsingEqual);
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
   * 解析复合标签或者函数语法的函数，不会解析其他类型如数字、列表等。与 {@link #parseCompound} 不同的是，仍会解析函数语法和引用语法，尽管这些情况下的 NBT 函数运行的结果不一定是复合标签。
   */
  public static <S> NbtFunction parsePreferringCompound(ParseContext<S> parseContext, boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    // 解析等号和不等号
    final boolean isUsingEqual = NbtFunctionParser.parseBeforeFunctionName(parseContext, mustExpectSign, equalsForDefault);

    final NbtFunction functionGrammar = NbtFunctionParser.parseFunctionGrammar(parseContext);
    if (functionGrammar != null) return functionGrammar;

    final ReferenceNbtFunction reference = ReferenceNbtFunction.PREFIXED_ID_PARSER.parse(parseContext);
    if (reference != null) return reference;

    parseContext.terminateSuggestionsIfNotEmpty();
    parseContext.addSuggestion((context, suggestionsBuilder) -> NbtParserShared.suggestCompoundStart(suggestionsBuilder));
    return parseCompound(parseContext, isUsingEqual);
  }

  private static @Nullable <S> NbtFunction parseFunctionGrammar(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorBeforeFunctionName = reader.getCursor();
    final NbtFunction functionGrammar = NbtFunctionParsing.FUNCTIONS_PARSER.parse(parseContext);
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

  private static <S> boolean parseBeforeFunctionName(ParseContext<S> parseContext, boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorBeforeSign = reader.getCursor();
    final boolean isUsingEqual = parseSign(parseContext, mustExpectSign, equalsForDefault);

    final boolean hasExplicitSign = cursorBeforeSign != reader.getCursor();
    reader.skipWhitespace();
    if (hasExplicitSign) {
      parseContext.clearSuggestion();
    }
    return isUsingEqual;
  }
}
