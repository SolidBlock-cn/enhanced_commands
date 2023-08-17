package pers.solid.ecmd.argument;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.util.SuggestionProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class KeywordArgsArgumentType implements ArgumentType<KeywordArgs> {
  public static final DynamicCommandExceptionType UNKNOWN_ARGUMENT_NAME = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.keyword_args.unknown_argument_name", o));
  public static final DynamicCommandExceptionType DUPLICATE_ARGUMENT_NAME = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.keyword_args.duplicate_argument_name", o));
  public final @Unmodifiable Map<@NotNull String, ArgumentType<?>> arguments;
  public final @Unmodifiable Map<@NotNull String, Object> defaultValues;

  public KeywordArgsArgumentType(@Unmodifiable Map<String, ArgumentType<?>> arguments, @Unmodifiable Map<@NotNull String, Object> defaultValues) {
    this.arguments = arguments;
    this.defaultValues = defaultValues;
  }

  public static Builder keywordArgsBuilder() {
    return new Builder(new ImmutableMap.Builder<>(), new ImmutableMap.Builder<>());
  }

  public static KeywordArgs getKeywordArgs(String name, CommandContext<?> source) {
    return source.getArgument(name, KeywordArgs.class);
  }

  public static Builder keywordArgsBuilder(@NotNull KeywordArgsArgumentType copyFrom) {
    final Builder builder = new Builder(new ImmutableMap.Builder<>(), new ImmutableMap.Builder<>());
    builder.arguments.putAll(copyFrom.arguments);
    builder.defaultValues.putAll(copyFrom.defaultValues);
    return builder;
  }

  @Override
  public KeywordArgs parse(StringReader reader) throws CommandSyntaxException {
    final SuggestedParser parser = new SuggestedParser(reader);
    return parseAndSuggest(parser, false);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    final SuggestedParser suggestedParser = new SuggestedParser(new StringReader(builder.getInput()));
    suggestedParser.reader.setCursor(builder.getStart());
    try {
      parseAndSuggest(suggestedParser, true);
    } catch (CommandSyntaxException ignored) {
    }
    final var builder2 = builder.createOffset(suggestedParser.reader.getCursor());
    suggestedParser.suggestions.forEach(consumer -> consumer.accept(context, builder2));
    return builder2.buildFuture();
  }

  private KeywordArgs parseAndSuggest(SuggestedParser parser, boolean hasSuggestions) throws CommandSyntaxException {
    final Map<String, Object> values = new HashMap<>();
    final SuggestionProvider nameSuggestion = (context, builder) -> CommandSource.suggestMatching(arguments.keySet().stream().filter(s -> !values.containsKey(s)), builder);
    if (hasSuggestions) {
      parser.suggestions.clear();
      parser.suggestions.add(nameSuggestion);
    }

    for (int i = 0; i < 1024 && parser.reader.canRead(); i++) {
      // parse name
      final int cursorBeforeReadName = parser.reader.getCursor();
      final String name = parser.reader.readString();
      if (!arguments.containsKey(name)) {
        parser.reader.setCursor(cursorBeforeReadName);
        throw UNKNOWN_ARGUMENT_NAME.createWithContext(parser.reader, name);
      } else if (values.containsKey(name)) {
        parser.reader.setCursor(cursorBeforeReadName);
        throw DUPLICATE_ARGUMENT_NAME.createWithContext(parser.reader, name);
      }
      parser.reader.skipWhitespace();
      if (hasSuggestions) {
        parser.suggestions.clear();
        parser.suggestions.add((context, builder) -> builder.suggest("="));
      }
      parser.reader.expect('=');
      parser.reader.skipWhitespace();
      // parse value
      final ArgumentType<?> argumentType = arguments.get(name);
      if (hasSuggestions) {
        parser.suggestions.clear();
        parser.suggestions.add(argumentType::listSuggestions);
      }
      final Object parse = argumentType.parse(parser.reader);

      values.put(name, parse);

      parser.suggestions.clear();
      if (!parser.reader.canRead()) {
        // ensure there is at least a whitespace
        break;
      }
      if (hasSuggestions) {
        parser.suggestions.add(nameSuggestion);
      }
      parser.reader.skipWhitespace();
    }
    return new KeywordArgs(this, Map.copyOf(values));
  }

  public static class Builder {
    private final ImmutableMap.Builder<String, ArgumentType<?>> arguments;
    private final ImmutableMap.Builder<String, Object> defaultValues;

    public Builder(ImmutableMap.Builder<String, ArgumentType<?>> arguments, ImmutableMap.Builder<String, Object> defaultValues) {
      this.arguments = arguments;
      this.defaultValues = defaultValues;
    }

    public Builder addRequiredArg(@NotNull String name, @NotNull ArgumentType<?> type) {
      arguments.put(name, type);
      return this;
    }

    public <T> Builder addOptionalArg(@NotNull String name, @NotNull ArgumentType<T> type, T defaultValue) {
      arguments.put(name, type);
      defaultValues.put(name, defaultValue);
      return this;
    }

    public KeywordArgsArgumentType builder() {
      return new KeywordArgsArgumentType(arguments.build(), defaultValues.build());
    }
  }

}
