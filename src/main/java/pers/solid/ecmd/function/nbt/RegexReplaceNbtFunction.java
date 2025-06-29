package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.NamedParamListParser;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 使用正则表达式替换 NBT 字符串内容的 NBT 函数。
 *
 * @param pattern     正则表达式。
 * @param replacement 被替换后的内容。
 * @param recursive   是否递归替换，如果为 true，那么当参数为复合标签或列表时，其包含的各字符串都被替换。
 * @param lenient     如果为 true，那么当被替换的内容不是字符串且 recursive 不为 true，或者当 replacement 中包含无效的组号时，不会抛出异常。
 * @param original    指定被替换前的 NBT 数据。如果未指定，则默认使用 NBT 函数运行时的参数。
 */
public record RegexReplaceNbtFunction(Pattern pattern, String replacement, boolean recursive, boolean lenient, Optional<NbtFunction> original) implements NbtFunction {
  public static final MapCodec<RegexReplaceNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      CodecUtil.PATTERN.fieldOf("pattern").forGetter(RegexReplaceNbtFunction::pattern),
      Codec.STRING.fieldOf("replacement").forGetter(RegexReplaceNbtFunction::replacement),
      Codec.BOOL.optionalFieldOf("recursive", false).forGetter(RegexReplaceNbtFunction::recursive),
      Codec.BOOL.optionalFieldOf("lenient", false).forGetter(RegexReplaceNbtFunction::lenient),
      NbtFunction.CODEC.optionalFieldOf("original").forGetter(RegexReplaceNbtFunction::original)
  ).apply(i, RegexReplaceNbtFunction::new));

  @Override
  public @NotNull String asString() {
    return "regex.replace(" + pattern.pattern() + ", " + NbtString.escape(replacement) + "; recursive = " + recursive + ", lenient = " + lenient + original.map(nbtFunction -> "; original = " + nbtFunction.asString()).orElse("") + ")";
  }

  @Override
  public @NotNull NbtFunctionType<RegexReplaceNbtFunction> getType() {
    return Type.REGEX_TYPE;
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement, ExecutionContext context) throws CommandSyntaxException {
    if (original.isPresent()) {
      nbtElement = original.get().apply(nbtElement, context);
    }
    if (recursive) {
      return NbtFunction.recursivelyApply(e -> {
        if (e instanceof NbtString nbtString) {
          final String string = nbtString.asString();
          try {
            return NbtString.of(pattern.matcher(string).replaceAll(replacement));
          } catch (RuntimeException ex) {
            if (lenient) {
              return e;
            } else {
              throw ModCommandExceptionTypes.INVALID_REGEX.create(ex.getMessage());
            }
          }
        }
        return null;
      }, nbtElement, null);
    }
    if (nbtElement instanceof NbtString nbtString) {
      final String string = nbtString.asString();
      try {
        return NbtString.of(pattern.matcher(string).replaceAll(replacement));
      } catch (RuntimeException ex) {
        if (lenient) {
          return nbtElement;
        } else {
          throw ModCommandExceptionTypes.INVALID_REGEX.create(ex.getMessage());
        }
      }
    } else if (lenient && nbtElement != null) {
      // 在当 lenient 为 true 时，不会报错。但仅限 nbtElement 为非 null 的情况下。
      return nbtElement;
    } else {
      // handle absent value
      if (nbtElement == null) {
        throw StringReplaceNbtFunction.NO_VALUE.create();
      }
      throw StringReplaceNbtFunction.NOT_A_STRING.create(nbtElement.getNbtType().getCommandFeedbackName());
    }
  }

  public enum Type implements NbtFunctionType<RegexReplaceNbtFunction> {
    REGEX_TYPE;

    @Override
    public MapCodec<RegexReplaceNbtFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionLikeParser<RegexReplaceNbtFunction>, NamedParamListParser {
    private static final Set<String> SUPPORTED = Set.of("recursive", "lenient", "original");
    private Pattern regex;
    private String replacement;
    private Boolean recursive, lenient;
    private NbtFunction original;

    @Override
    public RegexReplaceNbtFunction getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return new RegexReplaceNbtFunction(regex, replacement, Boolean.TRUE.equals(recursive), Boolean.TRUE.equals(lenient), Optional.ofNullable(original));
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      regex = ParsingUtil.readRegex(reader);
      reader.skipWhitespace();
      reader.expect(',');
      reader.skipWhitespace();
      replacement = reader.readString();
      reader.skipWhitespace();

      if (reader.canRead() && reader.peek() == ';') {
        reader.skip();
        reader.skipWhitespace();

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
        case "recursive" -> recursive != null;
        case "lenient" -> lenient != null;
        case "original" -> original != null;
        default -> false;
      };
    }

    @Override
    public void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException {
      switch (paramName) {
        case "recursive" -> {
          parseContext.addSuggestion((context, builder) -> ParsingUtil.suggestBoolean(builder));
          recursive = parseContext.reader().readBoolean();
        }
        case "lenient" -> {
          parseContext.addSuggestion((context, builder) -> ParsingUtil.suggestBoolean(builder));
          lenient = parseContext.reader().readBoolean();
        }
        case "original" -> original = NbtFunction.parse(parseContext, false, false);
      }
    }
  }
}
