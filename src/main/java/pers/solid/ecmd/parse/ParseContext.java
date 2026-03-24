package pers.solid.ecmd.parse;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.StringRepresentable;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * <p>命令解析时的环境，包括解析时所使用的 {@code StringReader}、存储建议的列表以及解析时的一些选项，可用于在解析的同时提供建议，相当于一次性完成 {@link ArgumentType#parse(StringReader)} 和 {@link ArgumentType#listSuggestions(CommandContext, SuggestionsBuilder)} 的两个工作，但需要注意的是，对于 {@link ArgumentType} 而言，这个解析过程仍会运行两遍，一遍用于解析结果，一遍用于提供建议。为了更灵活地控制建议提供过程，此类允许一次性提供多个建议。解析过程结束时（包括抛出 {@link CommandSyntaxException} 时），{@link #reader} 所在的 {@link StringReader#cursor cursor} 就是调用 {@link SuggestionProvider} 的 {offset} 的初始位置，而这是位置也正是 {@link CommandSyntaxException} 的 cursor 位置。
 * <p>此类通常用作解析内容的函数的参数，存储其解析时的一些选项，如是否仅提供建议而非实际解析、是否允许分散的内容等。
 *
 * @param <S>             其 commandSource 的类型。
 * @param registries      常用于命令中，用于从注册表中获取一些信息，常见于方块、物品、实体等的 ID 解析过程中。
 * @param reader          {@link StringReader} 对象，会直接用于解析。在提供建议时，也会基于此对象的 {@link StringReader#string string} 和 {@link StringReader#cursor cursor} 来提供建议。
 * @param suggestions     存储解析过程中所需要提供的建议。解析的过程不提供具体的建议，只指定如何提供建议（{@link SuggestionProvider}）。可以提供多种不同的建议。
 * @param suggestionsOnly 解析过程中是否仅提供建议，而非实际进行解析。如果为 {@code true}，那么一些不影响后续解析过程的操作可以不进行。
 * @param allowSparse     对于特定类型的语法，是否允许各部分用空格隔开。一般来说，直接用作命令参数、外面没有括号时，是 {@code false}。如果是在括号（或有明显其他割开定界符的环境）内解析，则为 {@code true}。在解析内容时，如果设置到在括号等语法内解析另一个对象，则通常来说此字段应该是 {@code true}。例如，在直接作为命令参数时，方块函数 {@code a|b} 不能写成 {@code a | b}，但是在被括号括起来的情况下，添加空格则完全没有问题，例如 {@code (a|b)} 和 {@code (a | b)} 都正确。
 */
public record ParseContext<S>(HolderLookup.Provider registries, StringReader reader, List<SuggestionProvider<S>> suggestions, boolean suggestionsOnly, boolean allowSparse) {
  public ParseContext(String string) {
    this(new StringReader(string));
  }

  public ParseContext(StringReader reader) {
    this(null, reader, false, false);
  }

  public ParseContext(HolderLookup.Provider registries, String string, boolean suggestionsOnly, boolean allowSparse) {
    this(registries, new StringReader(string), suggestionsOnly, allowSparse);
  }

  public ParseContext(HolderLookup.Provider registries, StringReader reader, boolean suggestionsOnly, boolean allowSparse) {
    this(registries, reader, new ArrayList<>(), suggestionsOnly, allowSparse);
  }

  /**
   * 根据 {@link #suggestions} 中的内容提供建议。
   *
   * @see com.mojang.brigadier.CommandDispatcher#getCompletionSuggestions(ParseResults, int)
   */
  public static <S> CompletableFuture<Suggestions> buildSuggestions(List<SuggestionProvider<S>> suggestions, CommandContext<S> context, SuggestionsBuilder builder) {
    if (suggestions == null) {
      return Suggestions.empty();
    }
    final List<CompletableFuture<Suggestions>> completableFutures = new ArrayList<>();
    for (SuggestionProvider<S> suggestionProvider : suggestions) {
      if (suggestionProvider == Special.TERMINATE) {
        break;
      }
      try {
        completableFutures.add(suggestionProvider.getSuggestions(context, builder));
      } catch (CommandSyntaxException ignored) {
      }
    }
    if (completableFutures.isEmpty()) {
      return builder.buildFuture();
    } else if (completableFutures.size() == 1) {
      return completableFutures.getFirst();
    } else {
      return combineMultipleSuggestions(builder, completableFutures);
    }
  }

  public static @NotNull CompletableFuture<Suggestions> combineMultipleSuggestions(SuggestionsBuilder builder, Collection<CompletableFuture<Suggestions>> suggestionsList) {
    final CompletableFuture<Suggestions> result = new CompletableFuture<>();
    CompletableFuture.allOf(suggestionsList.toArray(CompletableFuture[]::new))
        .thenRun(() -> {
          final List<Suggestions> results = new ArrayList<>();
          for (final CompletableFuture<Suggestions> future : suggestionsList) {
            final Suggestions join = future.join();
            if (join == Special.TERMINATE_IF_NOT_EMPTY_SUGGESTIONS) {
              // future == null，表示需要标识当建议项不为 null 时，直接结束建议。
              if (!(results.isEmpty() || results.getLast().isEmpty())) {
                break;
              } else {
                continue;
              }
            }
            results.add(join);
          }
          result.complete(Suggestions.merge(builder.getInput(), results));
        });
    return result;
  }

  public ParseContext<S> withSuggestionsOnly(boolean suggestionsOnly) {
    if (this.suggestionsOnly == suggestionsOnly) {
      return this;
    }
    return new ParseContext<>(registries, reader, suggestions, suggestionsOnly, allowSparse);
  }

  public ParseContext<S> withAllowSparse(boolean allowSparse) {
    if (this.allowSparse == allowSparse) {
      return this;
    }
    return new ParseContext<>(registries, reader, suggestions, suggestionsOnly, allowSparse);
  }

  public void setSuggestion(SuggestionProvider<S> suggestion) {
    this.suggestions.clear();
    this.suggestions.add(suggestion);
  }

  public void addSuggestion(SuggestionProvider<S> suggestion) {
    this.suggestions.add(suggestion);
  }

  public @Unmodifiable List<SuggestionProvider<S>> getAllSuggestions() {
    return List.copyOf(suggestions);
  }

  public void replaceAllSuggestions(List<SuggestionProvider<S>> suggestions) {
    this.suggestions.clear();
    this.suggestions.addAll(suggestions);
  }

  public void terminateSuggestionsIfNotEmpty() {
    suggestions.add(Special.TERMINATE_IF_NOT_EMPTY.forceCast());
  }

  public void clearSuggestion() {
    this.suggestions.clear();
  }

  /**
   * 解析一个不同类型的角度时，并返回弧度值。角度值的单位可以是 {@code deg}、{@code rad} 或 {@code turn}。零值可以不提供单位，其他情况下不提供单位会抛出错误。输入完数值后会为单位提供建议。
   *
   * @param radians 返回的值是否为弧度值，若为 {@code false}，则返回角度值。
   */
  public double parseAndSuggestAngle(boolean radians) throws CommandSyntaxException {
    final StringReader reader = this.reader;
    final int cursorBeforeDouble = reader.getCursor();
    while (reader.canRead()) {
      final char peek = reader.peek();
      if ((peek < '0' || peek > '9') && peek != '-') {
        if (peek != '.') {
          break; // 无效字符
        } else if (reader.canRead(2) && reader.peek(1) == '.') {
          break; // 后面两个字符都是小数点，无效
        }
      }
      reader.skip();
    }
    final String substring = reader.getString().substring(cursorBeforeDouble, reader.getCursor());
    if (substring.isEmpty()) {
      this.reader.setCursor(cursorBeforeDouble);
      this.reader.readUnquotedString();
      final CommandSyntaxException exception = CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedDouble().createWithContext(reader);
      if (this.reader.getCursor() > cursorBeforeDouble) {
        throw EnhancedCommandSyntaxException.withCursorEnd(exception, this.reader.getCursor());
      } else {
        throw exception;
      }
    }
    final double v;
    try {
      v = Double.parseDouble(substring);
    } catch (NumberFormatException e) {
      final int cursorAfterNumber = reader.getCursor();
      this.reader.setCursor(cursorBeforeDouble);
      throw EnhancedCommandSyntaxException.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidDouble().createWithContext(reader, substring), cursorAfterNumber);
    }
    setSuggestion((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(List.of("deg", "rad", "turn"), suggestionsBuilder));
    final int cursorBeforeUnit = reader.getCursor();
    while (reader.canRead()) {
      final char peek = reader.peek();
      if (peek >= 'A' && peek <= 'Z' || peek >= 'a' && peek <= 'z') {
        reader.skip();
      } else {
        break;
      }
    }
    final String unit = reader.getString().substring(cursorBeforeUnit, reader.getCursor());
    if (unit.isEmpty()) {
      if (v == 0) {
        return 0;
      } else {
        reader.setCursor(cursorBeforeUnit);
        throw EnhancedCommandsCommandExceptionTypes.ANGLE_UNIT_EXPECTED.createWithContext(reader, substring);
      }
    } else if ("deg".equals(unit)) {
      clearSuggestion();
      return radians ? Math.toRadians(v) : v;
    } else if ("rad".equals(unit)) {
      clearSuggestion();
      return radians ? v : Math.toDegrees(v);
    } else if ("turn".equals(unit)) {
      clearSuggestion();
      return (radians ? Math.PI * 2 : 360) * v;
    } else {
      final int cursorAfterUnit = reader.getCursor();
      reader.setCursor(cursorBeforeUnit);
      throw EnhancedCommandSyntaxException.withCursorEnd(EnhancedCommandsCommandExceptionTypes.ANGLE_UNIT_UNKNOWN.createWithContext(reader, unit), cursorAfterUnit);
    }
  }

  /**
   * 根据 {@link #suggestions} 中的内容提供建议。
   *
   * @see com.mojang.brigadier.CommandDispatcher#getCompletionSuggestions(ParseResults, int)
   */
  public CompletableFuture<Suggestions> buildSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return buildSuggestions(suggestions, context, builder);
  }

  public <T> @NotNull T parseAndSuggestValues(Iterable<@NotNull T> iterable, Function<@NotNull T, String> suggestions, Function<@NotNull T, @Nullable Message> tooltip, FailableFunction<String, @Nullable T, CommandSyntaxException> valueGetter) throws CommandSyntaxException {
    setSuggestion((context, builder) -> SharedSuggestionProvider.suggest(iterable, builder, suggestions, tooltip));
    return ParsingUtil.parseValues(this.reader, valueGetter);
  }

  public <T extends Enum<T> & StringRepresentable> @NotNull T parseAndSuggestEnums(Iterable<@NotNull T> iterable, Function<@NotNull T, @Nullable Message> tooltip, FailableFunction<String, T, CommandSyntaxException> valueGetter) throws CommandSyntaxException {
    return parseAndSuggestValues(iterable, StringRepresentable::getSerializedName, tooltip, valueGetter);
  }

  public <T extends Enum<T> & StringRepresentable> @NotNull T parseAndSuggestEnums(Iterable<@NotNull T> iterable, Function<@NotNull T, @Nullable Message> tooltip, StringIdentifiableCodec<T> codec) throws CommandSyntaxException {
    return parseAndSuggestEnums(iterable, tooltip, codec::byId);
  }

  public <T extends Enum<T> & StringRepresentable> @NotNull T parseAndSuggestEnums(@NotNull T[] iterable, Function<@NotNull T, @Nullable Message> tooltip, StringIdentifiableCodec<T> codec) throws CommandSyntaxException {
    return parseAndSuggestEnums(Arrays.asList(iterable), tooltip, codec);
  }

  /**
   * 通过指定的 {@link ArgumentType} 解析其对应的值并提供建议。调用此函数时，会确保 {@link SuggestionsBuilder} 的位置合理。
   */
  public <T> T parseAndSuggestArgument(ArgumentType<T> argumentType) throws CommandSyntaxException {
    final int cursorBeforeParse = reader.getCursor();
    addSuggestion((context, builder) -> {
      final SuggestionsBuilder builderOffset = builder.createOffset(cursorBeforeParse);
      return argumentType.listSuggestions(context, builderOffset);
    });
    return argumentType.parse(reader);
  }

  public enum Special implements SuggestionProvider<Void> {
    TERMINATE, TERMINATE_IF_NOT_EMPTY;
    public static final Suggestions TERMINATE_IF_NOT_EMPTY_SUGGESTIONS = new Suggestions(StringRange.at(114514), List.of());

    @SuppressWarnings("unchecked")
    public <S> SuggestionProvider<S> forceCast() {
      return (SuggestionProvider<S>) this;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<Void> context, SuggestionsBuilder builder) {
      return CompletableFuture.completedFuture(TERMINATE_IF_NOT_EMPTY_SUGGESTIONS);
    }
  }
}
