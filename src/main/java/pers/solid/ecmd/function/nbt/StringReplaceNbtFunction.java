package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * 对字符串进行替换的 NBT 函数。
 *
 * @param target      查找内容。
 * @param replacement 被替换后的内容。
 * @param recursive   是否递归替换，如果为 true，那么当参数为复合标签或列表时，其包含的各字符串都被替换。
 * @param lenient     如果为 true，那么当被替换的内容不是字符串且 recursive 不为 true 时。
 * @param original    指定被替换前的 NBT 数据。如果未指定，则默认使用 NBT 函数运行时的参数。
 */
public record StringReplaceNbtFunction(String target, String replacement, boolean recursive, boolean lenient, Optional<NbtFunction> original) implements NbtFunction {
  public static final DynamicCommandExceptionType NOT_A_STRING = new DynamicCommandExceptionType(s -> Component.translatable("enhanced_commands.nbt_function.string_replace.not_a_string", s));
  public static final SimpleCommandExceptionType NO_VALUE = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_function.string_replace.not_value"));

  public static final MapCodec<StringReplaceNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.STRING.fieldOf("target").forGetter(StringReplaceNbtFunction::target),
      Codec.STRING.fieldOf("replacement").forGetter(StringReplaceNbtFunction::replacement),
      Codec.BOOL.optionalFieldOf("recursive", false).forGetter(StringReplaceNbtFunction::recursive),
      Codec.BOOL.optionalFieldOf("lenient", false).forGetter(StringReplaceNbtFunction::lenient),
      NbtFunction.CODEC.optionalFieldOf("original").forGetter(StringReplaceNbtFunction::original)
  ).apply(i, StringReplaceNbtFunction::new));

  @Override
  public @NotNull String asString() {
    return "string_replace(" + StringTag.quoteAndEscape(target) + ", " + StringTag.quoteAndEscape(replacement) + ", recursive = " + recursive + ", lenient = " + lenient + original.map(nbtFunction -> ", original = " + nbtFunction.asString()).orElse("") + ")";
  }

  @Override
  public @NotNull NbtFunctionType<?> getType() {
    return Type.STRING_REPLACE_TYPE;
  }

  @Override
  public @NotNull Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    if (original.isPresent()) {
      nbtElement = original.get().apply(nbtElement, context);
    }
    if (recursive) {
      return NbtFunction.recursivelyApply(e -> e instanceof StringTag nbtString ? StringTag.valueOf(nbtString.getAsString().replace(target, replacement)) : null, nbtElement, null);
    }
    if (nbtElement instanceof StringTag nbtString) {
      final String string = nbtString.getAsString();
      return StringTag.valueOf(string.replace(target, replacement));
    } else if (lenient && nbtElement != null) {
      // 在当 lenient 为 true 时，不会报错。但仅限 nbtElement 为非 null 的情况下。
      return nbtElement;
    } else {
      // handle absent value
      if (nbtElement == null) {
        throw NO_VALUE.create();
      }
      throw NOT_A_STRING.create(nbtElement.getType().getPrettyName());
    }
  }

  public enum Type implements NbtFunctionType<StringReplaceNbtFunction> {
    STRING_REPLACE_TYPE;

    @Override
    public MapCodec<StringReplaceNbtFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionLikeParser.MixedParams<StringReplaceNbtFunction> {
    private static final Set<String> SUPPORTED = Set.of("recursive", "lenient", "original");
    private String target, replacement;
    private Boolean recursive, lenient;
    private NbtFunction original;

    @Override
    public StringReplaceNbtFunction getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return new StringReplaceNbtFunction(target, replacement, Boolean.TRUE.equals(recursive), Boolean.TRUE.equals(lenient), Optional.ofNullable(original));
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      switch (paramIndex) {
        case 0 -> target = parseContext.reader().readString();
        case 1 -> replacement = parseContext.reader().readString();
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
