package pers.solid.ecmd.argument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension.withCursorEnd;

public record KeywordArgsArgumentType(@Unmodifiable Map<@NotNull String, ArgumentType<?>> arguments, Set<String> requiredArguments, @Unmodifiable Map<@NotNull String, Object> defaultValues) implements ArgumentType<KeywordArgs> {
  public static final DynamicCommandExceptionType UNKNOWN_ARGUMENT_NAME = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.keyword_args.unknown_argument_name", o));
  public static final DynamicCommandExceptionType DUPLICATE_ARGUMENT_NAME = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.keyword_args.duplicate_argument_name", o));

  public static Builder builder() {
    return new Builder(new ImmutableMap.Builder<>(), new ImmutableSet.Builder<>(), new ImmutableMap.Builder<>());
  }

  public static Builder builder(@NotNull KeywordArgsArgumentType copyFrom) {
    return builder().addAll(copyFrom);
  }

  public static KeywordArgs getKeywordArgs(String name, CommandContext<?> context) {
    return context.getArgument(name, KeywordArgs.class);
  }

  public KeywordArgs defaultArgs() {
    return new KeywordArgs(this, defaultValues);
  }

  @Override
  public KeywordArgs parse(StringReader reader) throws CommandSyntaxException {
    return parseAndSuggest(reader, null);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    final StringReader reader = new StringReader(builder.getInput());
    reader.setCursor(builder.getStart());
    final MutableObject<SuggestionProvider<S>> mutableObject = new MutableObject<>();
    try {
      parseAndSuggest(reader, mutableObject);
    } catch (CommandSyntaxException ignored) {}
    if (mutableObject.getValue() != null) {
      try {
        return mutableObject.getValue().getSuggestions(context, builder);
      } catch (CommandSyntaxException ignored) {}
    }
    return Suggestions.empty();
  }

  private <S> KeywordArgs parseAndSuggest(StringReader reader, @Nullable final MutableObject<SuggestionProvider<S>> suggestionProvider) throws CommandSyntaxException {
    final Map<String, Object> values = new HashMap<>();
    final SuggestionProvider<S> nameSuggestion = (context, builder) -> CommandSource.suggestMatching(arguments.keySet().stream().filter(s -> !values.containsKey(s)), builder.createOffset(reader.getCursor()));
    if (suggestionProvider != null) {
      suggestionProvider.setValue(nameSuggestion);
    }

    for (int i = 0; i < 1024 && reader.canRead(); i++) {
      // parse name
      final int cursorBeforeReadName = reader.getCursor();
      final String name = reader.readString();
      if (!arguments.containsKey(name)) {
        final int cursorAfterReadName = reader.getCursor();
        reader.setCursor(cursorBeforeReadName);
        throw withCursorEnd(UNKNOWN_ARGUMENT_NAME.createWithContext(reader, name), cursorAfterReadName);
      } else if (values.containsKey(name)) {
        final int cursorAfterReadName = reader.getCursor();
        reader.setCursor(cursorBeforeReadName);
        throw withCursorEnd(DUPLICATE_ARGUMENT_NAME.createWithContext(reader, name), cursorAfterReadName);
      }
      reader.skipWhitespace();
      if (suggestionProvider != null) {
        final int cursorBeforeEqual = reader.getCursor();
        suggestionProvider.setValue((context, builder) -> builder.suggest("=").createOffset(cursorBeforeEqual).buildFuture());
      }
      reader.expect('=');
      reader.skipWhitespace();
      // parse value
      final ArgumentType<?> argumentType = arguments.get(name);
      if (suggestionProvider != null) {
        final int cursorBeforeParseValue = reader.getCursor();
        suggestionProvider.setValue((context, builder) -> argumentType.listSuggestions(context, builder.createOffset(cursorBeforeParseValue)));
      }
      final Object parse = argumentType.parse(reader);

      values.put(name, parse);

      if (suggestionProvider != null) {
        suggestionProvider.setValue(null);
      }
      if (!reader.canRead()) {
        // ensure there is at least a whitespace
        break;
      }
      if (suggestionProvider != null) {
        suggestionProvider.setValue(nameSuggestion);
      }
      reader.skipWhitespace();
    }
    return new KeywordArgs(this, Map.copyOf(values));
  }

  public static class Builder {
    private final ImmutableMap.Builder<String, ArgumentType<?>> arguments;
    private final ImmutableSet.Builder<String> requiredArguments;
    private final ImmutableMap.Builder<String, Object> defaultValues;

    private Builder(ImmutableMap.Builder<String, ArgumentType<?>> arguments, ImmutableSet.Builder<String> requiredArgs, ImmutableMap.Builder<String, Object> defaultValues) {
      this.arguments = arguments;
      this.requiredArguments = requiredArgs;
      this.defaultValues = defaultValues;
    }

    public Builder addRequiredArg(@NotNull String name, @NotNull ArgumentType<?> type) {
      arguments.put(name, type);
      requiredArguments.add(name);
      return this;
    }

    public <T> Builder addOptionalArg(@NotNull String name, @NotNull ArgumentType<T> type, @Nullable T defaultValue) {
      arguments.put(name, type);
      if (defaultValue != null) {
        defaultValues.put(name, defaultValue);
      }
      return this;
    }

    public Builder addAll(KeywordArgsArgumentType source) {
      arguments.putAll(source.arguments);
      requiredArguments.addAll(source.requiredArguments);
      defaultValues.putAll(source.defaultValues);
      return this;
    }

    public KeywordArgsArgumentType build() {
      return new KeywordArgsArgumentType(arguments.build(), requiredArguments.build(), defaultValues.build());
    }
  }
}
