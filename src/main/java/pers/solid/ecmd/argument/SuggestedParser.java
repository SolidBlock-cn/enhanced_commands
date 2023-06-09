package pers.solid.ecmd.argument;

import com.google.common.collect.Streams;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.SuggestionProvider;
import pers.solid.ecmd.util.SuggestionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SuggestedParser {
  public final CommandRegistryAccess commandRegistryAccess;

  public final StringReader reader;
  public List<SuggestionProvider> suggestions = new ArrayList<>();

  public SuggestedParser(CommandRegistryAccess commandRegistryAccess, StringReader reader) {
    this.commandRegistryAccess = commandRegistryAccess;
    this.reader = reader;
  }

  public CompletableFuture<Suggestions> buildSuggestions(CommandContext<?> context, SuggestionsBuilder builder) {
    for (SuggestionProvider suggestion : suggestions) {
      if (suggestion instanceof SuggestionProvider.Modifying modifying) {
        return modifying.apply(context, builder);
      } else {
        suggestion.accept(context, builder);
      }
    }
    return builder.buildFuture();
  }

  public <T> @NotNull T readAndSuggestValues(Iterable<@Nullable T> iterable, Function<T, String> suggestions, Function<T, @Nullable Message> tooltip, FailableFunction<String, T, CommandSyntaxException> valueGetter) throws CommandSyntaxException {
    final int cursorBeforeRead = reader.getCursor();
    this.suggestions.add((context, builder) -> SuggestionUtil.suggestMatchingWithTooltip(iterable, suggestions, tooltip, builder));
    final String name = reader.readString();
    final T value = valueGetter.apply(name);
    if (value == null) {
      reader.setCursor(cursorBeforeRead);
      throw UNKNOWN_VALUE.createWithContext(reader, name);
    } else {
      return value;
    }
  }

  public <T extends Enum<T> & StringIdentifiable> @NotNull T readAndSuggestEnums(Iterable<T> iterable, Function<T, @Nullable Message> tooltip, FailableFunction<String, T, CommandSyntaxException> valueGetter) throws CommandSyntaxException {
    return readAndSuggestValues(iterable, StringIdentifiable::asString, tooltip, valueGetter);
  }

  public static final DynamicCommandExceptionType UNKNOWN_VALUE = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.unknown_value", o));

  public <T extends Enum<T> & StringIdentifiable> @NotNull T readAndSuggestEnums(Iterable<T> iterable, Function<T, @Nullable Message> tooltip) throws CommandSyntaxException {
    return readAndSuggestEnums(iterable, tooltip, name -> Streams.stream(iterable).filter(t -> t.asString().equals(name)).findAny().orElseThrow(() -> UNKNOWN_VALUE.createWithContext(reader, name)));
  }

  public <T extends Enum<T> & StringIdentifiable> @NotNull T readAndSuggestEnums(Iterable<T> iterable, Function<T, @Nullable Message> tooltip, StringIdentifiable.Codec<T> codec) throws CommandSyntaxException {
    return readAndSuggestEnums(iterable, tooltip, codec::byId);
  }

  public <T extends Enum<T> & StringIdentifiable> @NotNull T readAndSuggestEnums(T[] iterable, Function<T, @Nullable Message> tooltip) throws CommandSyntaxException {
    return readAndSuggestEnums(Arrays.asList(iterable), tooltip);
  }

  public <T extends Enum<T> & StringIdentifiable> @NotNull T readAndSuggestEnums(T[] iterable, Function<T, @Nullable Message> tooltip, StringIdentifiable.Codec<T> codec) throws CommandSyntaxException {
    return readAndSuggestEnums(Arrays.asList(iterable), tooltip, codec);
  }
}
