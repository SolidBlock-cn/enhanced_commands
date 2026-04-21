package pers.solid.ecmd.nbt.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 此 NBT 函数用于截取子字符串。
 *
 * @param startIndex 开始索引，支持负数。
 * @param endIndex   结束索引，支持负数。
 * @param lenient    如果为 true，则当索引超出字符串范围时，也不抛出异常。
 * @param original   指定要截取子字符串的字符串，如果未指定，则默认使用 NBT 函数运行时的参数。
 */
public record SubstringNbtFunction(int startIndex, Optional<Integer> endIndex, boolean lenient, Optional<NbtFunction> original) implements NbtFunction {
  public static final MapCodec<SubstringNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.INT.fieldOf("start_index").forGetter(SubstringNbtFunction::startIndex),
      Codec.INT.optionalFieldOf("end_index").forGetter(SubstringNbtFunction::endIndex),
      Codec.BOOL.optionalFieldOf("lenient", false).forGetter(SubstringNbtFunction::lenient),
      NbtFunction.CODEC.optionalFieldOf("original").forGetter(SubstringNbtFunction::original)
  ).apply(i, SubstringNbtFunction::new));

  public static final DynamicCommandExceptionType NOT_A_STRING = new DynamicCommandExceptionType(s -> Component.translatable("enhanced_commands.nbt_function.substring.not_a_string", s));
  public static final SimpleCommandExceptionType NO_VALUE = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_function.substring.no_value"));
  public static final Dynamic2CommandExceptionType START_END_WRONG = new Dynamic2CommandExceptionType((start, end) -> Component.translatable("enhanced_commands.nbt_function.substring.start_end_wrong", start, end));
  public static final Dynamic2CommandExceptionType EXCEEDS_STRING_LENGTH = new Dynamic2CommandExceptionType((index, length) -> Component.translatable("enhanced_commands.nbt_function.substring.exceeds_string_length", index, length));
  public static final Dynamic2CommandExceptionType EXCEEDS_STRING_LENGTH_INVERSE = new Dynamic2CommandExceptionType((index, length) -> Component.translatable("enhanced_commands.nbt_function.substring.exceeds_string_length_inverse", index, length));

  @Override
  public String expressAsString() {
    return "substring(" + startIndex + (endIndex.isPresent() ? ", " + endIndex.get() : "") + ", lenient = " + lenient + original.map(nbtFunction -> ", original = " + nbtFunction.expressAsString()).orElse("") + ")";
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
  public NbtFunctionType<SubstringNbtFunction> getType() {
    return NbtFunctionTypes.SUBSTRING;
  }

  @Override
  public Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    if (original.isPresent()) {
      nbtElement = original.get().apply(nbtElement, context);
    }
    if (!(nbtElement instanceof StringTag nbtString)) {
      if (nbtElement == null) {
        throw NO_VALUE.create();
      } else if (lenient) {
        return nbtElement;
      } else {
        throw NOT_A_STRING.create(nbtElement.getType().getPrettyName());
      }
    }
    final String string = nbtString.getAsString();
    final int length = string.length();
    try {
      final int actualStartIndex = actualIndex(startIndex, length);
      if (endIndex.isEmpty()) {
        return StringTag.valueOf(string.substring(actualStartIndex));
      }
      final int actualEndIndex = actualIndex(endIndex.get(), length);
      if (actualStartIndex > actualEndIndex) {
        if (lenient) {
          return nbtElement;
        } else {
          throw START_END_WRONG.create(actualStartIndex, actualEndIndex);
        }
      }
      return StringTag.valueOf(string.substring(actualStartIndex, actualEndIndex));
    } catch (CommandSyntaxException e) {
      if (lenient) {
        return nbtElement;
      } else {
        throw e;
      }
    }
  }

  public static class Parser implements FunctionContentParser.MixedParams<SubstringNbtFunction> {
    private @Nullable Integer startIndex, endIndex;
    private @Nullable Boolean lenient;
    private @Nullable NbtFunction original;
    private static final Collection<String> SUPPORTED = Set.of("lenient", "original");

    @Override
    public SubstringNbtFunction getParseResult(ParseContext<?> parseContext) {
      final Optional<Integer> endIndex = Optional.ofNullable(this.endIndex);
      Objects.requireNonNull(startIndex, "startIndex");
      return new SubstringNbtFunction(startIndex, endIndex, Boolean.TRUE.equals(lenient), Optional.ofNullable(original));
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
        case "original" -> original = NbtFunction.parse(parseContext, false, false);
      }
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      switch (paramIndex) {
        case 0 -> startIndex = parseContext.reader().readInt();
        case 1 -> endIndex = parseContext.reader().readInt();
      }
    }

    @Override
    public int minSequentialParamsCount() {
      return 1;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 2;
    }
  }
}
