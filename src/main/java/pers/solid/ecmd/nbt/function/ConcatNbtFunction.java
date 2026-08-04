package pers.solid.ecmd.nbt.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.NamedParamListParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.RequiresValidation;

import java.util.*;
import java.util.stream.Collectors;

public sealed interface ConcatNbtFunction extends NbtFunction, RequiresValidation {
  DynamicCommandExceptionType CONCAT_ELEMENT_INVALID = new DynamicCommandExceptionType(t -> Component.translatable("enhanced_commands.nbt_function.concat.concat_element_invalid", t));
  DynamicCommandExceptionType CONCAT_NOT_LIST = new DynamicCommandExceptionType(t -> Component.translatable("enhanced_commands.nbt_function.concat.concat_not_list", t));
  MapCodec<ConcatNbtFunction> CODEC = Codec.BOOL.dispatchMap("flatten", ConcatNbtFunction::flatten, flatten -> flatten ? Flattened.CODEC : Direct.CODEC);

  private static String nbtToString(Tag nbtElement) throws CommandSyntaxException {
    if (nbtElement instanceof StringTag nbtString) {
      return nbtString.getAsString();
    } else if (nbtElement instanceof NumericTag number) {
      return number.getAsNumber().toString();
    } else {
      throw CONCAT_ELEMENT_INVALID.create(nbtElement.getType().getPrettyName());
    }
  }

  Optional<NbtFunction> delimiter();

  boolean flatten();

  default String delimiterString(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    if (delimiter().isPresent()) {
      return nbtToString(delimiter().get().apply(nbtElement, context));
    } else {
      return "";
    }
  }

  @Override
  default NbtFunctionType<ConcatNbtFunction> getType() {
    return NbtFunctionTypes.CONCAT;
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
    public String expressAsString() {
      return "concat(" + elements.stream().map(NbtFunction::expressAsString).collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
      final StringJoiner joiner = new StringJoiner(delimiterString(nbtElement, context));
      for (NbtFunction element : elements) {
        joiner.add(nbtToString(element.apply(nbtElement, context)));
      }
      return StringTag.valueOf(joiner.toString());
    }

    @Override
    public Iterable<? extends @Nullable Object> membersToValidate() {
      return Iterables.concat(elements, delimiter.stream().toList());
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
    public String expressAsString() {
      return "concat(* " + element.expressAsString() + ")";
    }

    @Override
    public Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
      final Tag applied = element.apply(nbtElement, context);
      if (applied instanceof ListTag nbtList) {
        final String delimiterString;
        if (delimiter.isPresent()) {
          delimiterString = nbtToString(delimiter.get().apply(nbtElement, context));
        } else {
          delimiterString = "";
        }
        final StringJoiner joiner = new StringJoiner(delimiterString);
        for (Tag element : nbtList) {
          joiner.add(nbtToString(element));
        }
        return StringTag.valueOf(joiner.toString());
      } else {
        throw CONCAT_NOT_LIST.create(applied.getType().getPrettyName());
      }
    }

    @Override
    public Iterable<? extends @Nullable Object> membersToValidate() {
      return Arrays.asList(element, delimiter.orElse(null));
    }
  }

  class Parser implements FunctionContentParser<ConcatNbtFunction>, NamedParamListParser {
    private static final Set<String> SUPPORTED_PARAMS = Set.of("delimiter");
    private final List<NbtFunction> nbtFunctions = new ArrayList<>();
    private boolean flatten;
    private @Nullable NbtFunction delimiter = null;

    @Override
    public ConcatNbtFunction getParseResult(ParseContext<?> parseContext) {
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
        nbtFunctions.add(NbtFunction.parse(parseContext, false, false));
      } else if (reader.canRead() && reader.peek() == ')') {
        parseContext.clearSuggestion();
      } else {
        while (true) {
          nbtFunctions.add(NbtFunction.parse(parseContext, false, false));
          reader.skipWhitespace();

          parseContext.addSuggestion((context, suggestionsBuilder) -> suggestionsBuilder
              .suggest(",").buildFuture());
          if (!reader.canRead()) {
            break;
          }
          reader.skipWhitespace();
          final char peek = reader.peek();
          if (peek == ',') {
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
        delimiter = NbtFunction.parse(parseContext, false, false);
      }
    }
  }
}
