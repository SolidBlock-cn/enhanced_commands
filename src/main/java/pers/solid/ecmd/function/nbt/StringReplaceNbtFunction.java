package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.NamedParamListParser;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public record StringReplaceNbtFunction(String target, String replacement, boolean recursive, boolean lenient, Optional<NbtFunction> original) implements NbtFunction {
  public static final DynamicCommandExceptionType NOT_A_STRING = new DynamicCommandExceptionType(s -> Text.translatable("enhanced_commands.nbt_predicate.string_replace.not_a_string", s));
  public static final SimpleCommandExceptionType NO_VALUE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.nbt_predicate.string_replace.not_value"));

  public static final MapCodec<StringReplaceNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.STRING.fieldOf("target").forGetter(StringReplaceNbtFunction::target),
      Codec.STRING.fieldOf("replacement").forGetter(StringReplaceNbtFunction::replacement),
      Codec.BOOL.optionalFieldOf("recursive", false).forGetter(StringReplaceNbtFunction::recursive),
      Codec.BOOL.optionalFieldOf("lenient", false).forGetter(StringReplaceNbtFunction::lenient),
      NbtFunction.CODEC.optionalFieldOf("original").forGetter(StringReplaceNbtFunction::original)
  ).apply(i, StringReplaceNbtFunction::new));

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return "string.replace(" + NbtString.escape(target) + ", " + NbtString.escape(replacement) + "; recursive = " + recursive + ", lenient = " + lenient + original.map(nbtFunction -> "; original = " + nbtFunction.asString(false)).orElse("") + ")";
  }

  @Override
  public @NotNull NbtFunctionType<?> getType() {
    return Type.STRING_REPLACE_TYPE;
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement, ExecutionContext context) throws CommandSyntaxException {
    if (original.isPresent()) {
      nbtElement = original.get().apply(nbtElement, context);
    }
    if (recursive) {
      return NbtFunction.recursivelyApply(e -> e instanceof NbtString nbtString ? NbtString.of(nbtString.asString().replace(target, replacement)) : null, nbtElement, null);
    }
    if (nbtElement instanceof NbtString nbtString) {
      final String string = nbtString.asString();
      return NbtString.of(string.replace(target, replacement));
    } else if (lenient && nbtElement != null) {
      // 在当 lenient 为 true 时，不会报错。但仅限 nbtElement 为非 null 的情况下。
      return nbtElement;
    } else {
      // handle absent value
      if (nbtElement == null) {
        throw NO_VALUE.create();
      }
      throw NOT_A_STRING.create(nbtElement.getNbtType().getCommandFeedbackName());
    }
  }

  public enum Type implements NbtFunctionType<StringReplaceNbtFunction> {
    STRING_REPLACE_TYPE;

    @Override
    public MapCodec<StringReplaceNbtFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionLikeParser<NbtFunctionArgument>, NamedParamListParser {
    private static final Set<String> SUPPORTED = Set.of("recursive", "lenient", "original");
    private String target, replacement;
    private Boolean recursive, lenient;
    private NbtFunctionArgument original;

    @Override
    public NbtFunctionArgument getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return source -> new StringReplaceNbtFunction(target, replacement, Boolean.TRUE.equals(recursive), Boolean.TRUE.equals(lenient), original == null ? Optional.empty() : Optional.of(original.toAbsolute(source)));
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      target = reader.readString();
      reader.skipWhitespace();
      reader.expect(',');
      reader.skipWhitespace();
      replacement = reader.readString();
      reader.skipWhitespace();

      if (reader.canRead() && reader.peek() == ';') {
        reader.skip();
        reader.skipWhitespace();

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
        case "target" -> target != null;
        case "replacement" -> replacement != null;
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
        case "original" -> original = NbtFunctionArgument.parse(parseContext, false, false);
      }
    }
  }
}
