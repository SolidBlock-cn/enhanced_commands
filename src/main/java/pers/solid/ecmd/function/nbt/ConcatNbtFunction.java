package pers.solid.ecmd.function.nbt;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.predicate.block.ExecutionContext;
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.NamedParamListParser;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.*;
import java.util.stream.Collectors;

public sealed interface ConcatNbtFunction extends NbtFunction {
  DynamicCommandExceptionType CONCAT_ELEMENT_INVALID = new DynamicCommandExceptionType(t -> Text.translatable("enhanced_commands.nbt_function.concat.concat_element_invalid", t));
  DynamicCommandExceptionType CONCAT_NOT_LIST = new DynamicCommandExceptionType(t -> Text.translatable("enhanced_commands.nbt_function.concat.concat_not_list", t));
  MapCodec<ConcatNbtFunction> CODEC = Codec.BOOL.dispatchMap("flatten", ConcatNbtFunction::flatten, flatten -> flatten ? Flattened.CODEC : Direct.CODEC);

  private static String nbtToString(NbtElement nbtElement) throws CommandSyntaxException {
    if (nbtElement instanceof NbtString nbtString) {
      return nbtString.asString();
    } else if (nbtElement instanceof AbstractNbtNumber number) {
      return number.numberValue().toString();
    } else {
      throw CONCAT_ELEMENT_INVALID.create(nbtElement.getNbtType().getCommandFeedbackName());
    }
  }

  Optional<NbtFunction> delimiter();

  boolean flatten();

  default String delimiterString(NbtElement nbtElement, ExecutionContext context) throws CommandSyntaxException {
    if (delimiter().isPresent()) {
      return nbtToString(delimiter().get().apply(nbtElement, context));
    } else {
      return "";
    }
  }

  @Override
  @NotNull
  default NbtFunctionType<ConcatNbtFunction> getType() {
    return Type.CONCAT_TYPE;
  }


  enum Type implements NbtFunctionType<ConcatNbtFunction> {
    CONCAT_TYPE;

    @Override
    public MapCodec<ConcatNbtFunction> getCodec() {
      return CODEC;
    }
  }

  record Direct(List<NbtFunction> elements, Optional<NbtFunction> delimiter) implements ConcatNbtFunction {
    private static final MapCodec<Direct> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        NbtFunction.CODEC.listOf().optionalFieldOf("elements", ImmutableList.of()).forGetter(Direct::elements),
        NbtFunction.CODEC.optionalFieldOf("delimiter").forGetter(Direct::delimiter)
    ).apply(i, Direct::new));

    @Override
    public boolean flatten() {
      return false;
    }

    @Override
    public @NotNull String asString() {
      return "concat(" + elements.stream().map(NbtFunction::asString).collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public @NotNull NbtElement apply(@Nullable NbtElement nbtElement, ExecutionContext context) throws CommandSyntaxException {
      final StringJoiner joiner = new StringJoiner(delimiterString(nbtElement, context));
      for (NbtFunction element : elements) {
        joiner.add(nbtToString(element.apply(nbtElement, context)));
      }
      return NbtString.of(joiner.toString());
    }
  }

  record Flattened(NbtFunction element, Optional<NbtFunction> delimiter) implements ConcatNbtFunction {
    private static final MapCodec<Flattened> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        NbtFunction.CODEC.fieldOf("element").forGetter(Flattened::element),
        NbtFunction.CODEC.optionalFieldOf("delimiter").forGetter(Flattened::delimiter)
    ).apply(i, Flattened::new));

    @Override
    public boolean flatten() {
      return true;
    }

    @Override
    public @NotNull String asString() {
      return "concat(* " + element.asString() + ")";
    }

    @Override
    public @NotNull NbtElement apply(@Nullable NbtElement nbtElement, ExecutionContext context) throws CommandSyntaxException {
      final NbtElement applied = element.apply(nbtElement, context);
      if (applied instanceof NbtList nbtList) {
        final String delimiterString;
        if (delimiter.isPresent()) {
          delimiterString = nbtToString(delimiter.get().apply(nbtElement, context));
        } else {
          delimiterString = "";
        }
        final StringJoiner joiner = new StringJoiner(delimiterString);
        for (NbtElement element : nbtList) {
          joiner.add(nbtToString(element));
        }
        return NbtString.of(joiner.toString());
      } else {
        throw CONCAT_NOT_LIST.create(applied.getNbtType().getCommandFeedbackName());
      }
    }
  }

  class Parser implements FunctionLikeParser<ConcatNbtFunction>, NamedParamListParser {
    private static final Set<String> SUPPORTED_PARAMS = Set.of("delimiter");
    private final List<NbtFunction> nbtFunctions = new ArrayList<>();
    private boolean flatten;
    private @Nullable NbtFunction delimiter = null;

    @Override
    public ConcatNbtFunction getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      if (flatten) {
        return new Flattened(nbtFunctions.getFirst(), Optional.ofNullable(delimiter));
      } else {
        return new Direct(nbtFunctions, Optional.ofNullable(delimiter));
      }
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      parseContext.addSuggestion((context, builder) -> builder.suggest("*").suggest(")").buildFuture());
      if (reader.canRead() && reader.peek() == '*') {
        reader.skip();
        reader.skipWhitespace();
        parseContext.clearSuggestion();
        flatten = true;

        // 在展平模式下，只能读取一个参数
        nbtFunctions.add(NbtFunctionArgument.parse(parseContext, false, false));
      } else if (reader.canRead() && reader.peek() == ')') {
        parseContext.clearSuggestion();
      } else {
        while (true) {
          nbtFunctions.add(NbtFunctionArgument.parse(parseContext, false, false));
          reader.skipWhitespace();

          parseContext.addSuggestion((context, suggestionsBuilder) -> suggestionsBuilder
              .suggest(separatorString()).buildFuture());
          if (!reader.canRead()) {
            break;
          }
          reader.skipWhitespace();
          final char peek = reader.peek();
          if (peek == separator()) {
            reader.skip();
            reader.skipWhitespace();
          } else {
            break;
          }
        }
      }

      parseContext.addSuggestion((context, builder) -> builder.suggest(";").buildFuture());

      parseNamedParameters(parseContext);
    }

    @Override
    public @Unmodifiable Collection<String> supportedParams() {
      return SUPPORTED_PARAMS;
    }

    @Override
    public boolean isDuplicateParamName(String paramName) {
      return "delimiter".equals(paramName) && delimiter != null;
    }

    @Override
    public void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException {
      if ("delimiter".equals(paramName)) {
        delimiter = NbtFunctionArgument.parse(parseContext, false, false);
      }
    }
  }
}
