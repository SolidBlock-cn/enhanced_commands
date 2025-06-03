package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.predicate.block.ExecutionContext;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.NamedParamListParser;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Collection;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * 此 NBT 函数用于截取子字符串。
 *
 * @param startIndex 开始索引，支持负数。
 * @param endIndex   结束索引，支持负数。
 * @param lenient    如果为 true，则当索引超出字符串范围时，也不抛出异常。
 * @param original   指定要截取子字符串的字符串，如果未指定，则默认使用 NBT 函数运行时的参数。
 */
public record SubstringNbtFunction(int startIndex, OptionalInt endIndex, boolean lenient, Optional<NbtFunction> original) implements NbtFunction {
  public static final MapCodec<SubstringNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.INT.fieldOf("start_index").forGetter(SubstringNbtFunction::startIndex),
      CodecUtil.optionalIntFieldOf("end_index").forGetter(SubstringNbtFunction::endIndex),
      Codec.BOOL.optionalFieldOf("lenient", false).forGetter(SubstringNbtFunction::lenient),
      NbtFunction.CODEC.optionalFieldOf("original").forGetter(SubstringNbtFunction::original)
  ).apply(i, SubstringNbtFunction::new));

  public static final DynamicCommandExceptionType NOT_A_STRING = new DynamicCommandExceptionType(s -> Text.translatable("enhanced_commands.nbt_function.substring.not_a_string", s));
  public static final SimpleCommandExceptionType NO_VALUE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.nbt_function.substring.no_value"));
  public static final Dynamic2CommandExceptionType START_END_WRONG = new Dynamic2CommandExceptionType((start, end) -> Text.translatable("enhanced_commands.nbt_function.substring.start_end_wrong", start, end));
  public static final Dynamic2CommandExceptionType EXCEEDS_STRING_LENGTH = new Dynamic2CommandExceptionType((index, length) -> Text.translatable("enhanced_commands.nbt_function.substring.exceeds_string_length", index, length));
  public static final Dynamic2CommandExceptionType EXCEEDS_STRING_LENGTH_INVERSE = new Dynamic2CommandExceptionType((index, length) -> Text.translatable("enhanced_commands.nbt_function.substring.exceeds_string_length_inverse", index, length));

  @Override
  public @NotNull String asString() {
    return "substring(" + startIndex + (endIndex.isPresent() ? ", " + endIndex.getAsInt() : "") + "; lenient = " + lenient + original.map(nbtFunction -> ", original = " + nbtFunction.asString()).orElse("") + ")";
  }

  private static int actualIndex(int index, int length) throws CommandSyntaxException {
    if (index > length) {
      throw EXCEEDS_STRING_LENGTH.create(index, length);
    }
    if (index < 0 && length + index < 0) {
      throw EXCEEDS_STRING_LENGTH_INVERSE.create(index, length);
    }
    return index >= 0 ? index : length + index;
  }

  @Override
  public @NotNull NbtFunctionType<SubstringNbtFunction> getType() {
    return Type.SUBSTRING_TYPE;
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement, ExecutionContext context) throws CommandSyntaxException {
    if (original.isPresent()) {
      nbtElement = original.get().apply(nbtElement, context);
    }
    if (!(nbtElement instanceof NbtString nbtString)) {
      if (nbtElement == null) {
        throw NO_VALUE.create();
      } else if (lenient) {
        return nbtElement;
      } else {
        throw NOT_A_STRING.create(nbtElement.getNbtType().getCommandFeedbackName());
      }
    }
    final String string = nbtString.asString();
    final int length = string.length();
    try {
      final int actualStartIndex = actualIndex(startIndex, length);
      if (endIndex.isEmpty()) {
        return NbtString.of(string.substring(actualStartIndex));
      }
      final int actualEndIndex = actualIndex(endIndex.getAsInt(), length);
      if (actualStartIndex > actualEndIndex) {
        if (lenient) {
          return nbtElement;
        } else {
          throw START_END_WRONG.create(actualStartIndex, actualEndIndex);
        }
      }
      return NbtString.of(string.substring(actualStartIndex, actualEndIndex));
    } catch (CommandSyntaxException e) {
      if (lenient) {
        return nbtElement;
      } else {
        throw e;
      }
    }
  }

  public enum Type implements NbtFunctionType<SubstringNbtFunction> {
    SUBSTRING_TYPE;

    @Override
    public MapCodec<SubstringNbtFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionLikeParser<NbtFunctionArgument>, NamedParamListParser {
    private Integer startIndex, endIndex;
    private Boolean lenient;
    private @Nullable NbtFunctionArgument original;
    private static final Collection<String> SUPPORTED = Set.of("lenient", "original");

    @Override
    public NbtFunctionArgument getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      final OptionalInt optionalInt = endIndex == null ? OptionalInt.empty() : OptionalInt.of(endIndex);
      return source -> new SubstringNbtFunction(startIndex, optionalInt, Boolean.TRUE.equals(lenient), original == null ? Optional.empty() : Optional.of(original.toAbsolute(source)));
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      startIndex = reader.readInt();
      parseContext.addSuggestion((context, builder) -> builder.suggest(",").suggest(";").buildFuture());

      if (!reader.canRead()) {
        throw ModCommandExceptionTypes.EXPECTED_2_SYMBOLS.create(",", ";");
      } else if (reader.peek() == ';') {
        // 直接跳过此 if 语句
      } else if (reader.peek() != ',') {
        parseContext.clearSuggestion();
        return;
      } else {
        // 以下为 reader.peek() == ',' 时

        reader.skip();
        reader.skipWhitespace();
        parseContext.clearSuggestion();

        endIndex = reader.readInt();
        reader.skipWhitespace();
        parseContext.addSuggestion((context, builder) -> builder.suggest(";").buildFuture());
      }

      if (reader.canRead() && reader.peek() == ';') {
        reader.skip();
        reader.skipWhitespace();
        parseContext.clearSuggestion();

        if (reader.canRead() && reader.peek() == ')') {
          return;
        }
        parseNamedParameters(parseContext);
      }
    }

    @Override
    public @Unmodifiable Collection<String> supportedParams() {
      return SUPPORTED;
    }

    @Override
    public boolean isDuplicateParamName(String paramName) {
      return switch (paramName) {
        case "lenient" -> lenient != null;
        case "original" -> original != null;
        default -> false;
      };
    }

    @Override
    public void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException {
      switch (paramName) {
        case "lenient" -> {
          parseContext.setSuggestion((context, builder) -> ParsingUtil.suggestBoolean(builder));
          lenient = parseContext.reader().readBoolean();
        }
        case "original" -> original = NbtFunctionArgument.parse(parseContext, false, false);
      }
    }
  }
}
