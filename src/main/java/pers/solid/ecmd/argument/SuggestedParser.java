package pers.solid.ecmd.argument;

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
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * <p>此类用于在解析一段内容的同时提供建议，相当于一次性完成 {@link ArgumentType#parse(StringReader)} 和 {@link ArgumentType#listSuggestions(CommandContext, SuggestionsBuilder)} 的两个工作，但需要注意的是，对于 {@link ArgumentType} 而言，这个解析过程仍会运行两遍，一遍用于解析结果，一遍用于提供建议。
 * <p>为了更灵活地控制建议提供过程，此类允许一次性提供多个建议。解析过程结束时（包括抛出 {@link CommandSyntaxException} 时），{@link #reader} 所在的 {@link StringReader#cursor cursor} 就是调用 {@link SuggestionProvider} 的 {offset} 的初始位置，而这是位置也正是 {@link CommandSyntaxException} 的 cursor 位置。
 */
public class SuggestedParser<S> {
  /**
   * 此对象的基于的 {@link StringReader} 对象，会直接用于解析。在提供建议时，也会基于此对象的 {@link StringReader#string string} 和 {@link StringReader#cursor cursor} 来提供建议。
   */
  public final StringReader reader;
  /**
   * 在当前解析过程中所需要提供的建议。解析的过程不提供具体的建议，只指定如何提供建议（{@link SuggestionProvider}）。可以提供多种不同的建议。
   */
  protected final List<SuggestionProvider<S>> suggestions;

  public SuggestedParser(String string) {
    this(new StringReader(string));
  }

  public SuggestedParser(StringReader reader) {
    this(reader, new ArrayList<>());
  }

  protected SuggestedParser(StringReader reader, List<SuggestionProvider<S>> suggestions) {
    this.reader = reader;
    this.suggestions = suggestions;
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

  public void clearSuggestion() {
    this.suggestions.clear();
  }

  /**
   * 解析整数的向量。这不是代表一个坐标，因此也不支持绝对坐标和局部坐标。
   */
  public Function<ServerCommandSource, Vec3i> parseAndSuggestVec3i() throws CommandSyntaxException {
    final StringReader reader = this.reader;
    {
      setSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = this.reader.readUnquotedString();
      final DirectionArgument byName = DirectionArgument.CODEC.byId(unquotedString);
      if (byName != null) {
        clearSuggestion();
        return source -> byName.apply(source).getVector();
      } else {
        this.reader.setCursor(cursorBeforeDirection);
      }
    }
    final int x = reader.readInt();
    clearSuggestion();
    ParsingUtil.expectAndSkipWhitespace(reader);
    {
      setSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = this.reader.readUnquotedString();
      final DirectionArgument byName = DirectionArgument.CODEC.byId(unquotedString);
      if (byName != null) {
        clearSuggestion();
        return source -> byName.apply(source).getVector().multiply(x);
      } else {
        this.reader.setCursor(cursorBeforeDirection);
      }
    }
    final int y = reader.readInt();
    clearSuggestion();
    ParsingUtil.expectAndSkipWhitespace(reader);
    final int z = reader.readInt();
    final Vec3i vec3i = new Vec3i(x, y, z);
    return source -> vec3i;
  }

  /**
   * 解析双精度浮点数的向量。这不是代表一个坐标，因此也不支持绝对坐标和局部坐标。形式为 {@code (<x> <y> <z> | [length] <direction>)}。
   */
  public Function<ServerCommandSource, Vec3d> parseAndSuggestVec3d() throws CommandSyntaxException {
    final StringReader reader = this.reader;
    {
      setSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = reader.readUnquotedString();
      final DirectionArgument byName = DirectionArgument.CODEC.byId(unquotedString);
      if (byName != null) {
        clearSuggestion();
        return source -> Vec3d.of(byName.apply(source).getVector());
      } else {
        reader.setCursor(cursorBeforeDirection);
      }
    }
    final double x = reader.readDouble();
    clearSuggestion();
    ParsingUtil.expectAndSkipWhitespace(reader);
    {
      setSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = reader.readUnquotedString();
      final DirectionArgument byName = DirectionArgument.CODEC.byId(unquotedString);
      if (byName != null) {
        clearSuggestion();
        return source -> Vec3d.of(byName.apply(source).getVector()).multiply(x);
      } else {
        reader.setCursor(cursorBeforeDirection);
      }
    }
    final double y = reader.readDouble();
    clearSuggestion();
    ParsingUtil.expectAndSkipWhitespace(reader);
    final double z = reader.readDouble();
    final Vec3d vec3d = new Vec3d(x, y, z);
    return source -> vec3d;
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
        throw CommandSyntaxExceptionExtension.withCursorEnd(exception, this.reader.getCursor());
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
      throw CommandSyntaxExceptionExtension.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidDouble().createWithContext(reader, substring), cursorAfterNumber);
    }
    setSuggestion((context, suggestionsBuilder) -> CommandSource.suggestMatching(List.of("deg", "rad", "turn"), suggestionsBuilder));
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
        throw ModCommandExceptionTypes.ANGLE_UNIT_EXPECTED.createWithContext(reader, substring);
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
      throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.ANGLE_UNIT_UNKNOWN.createWithContext(reader, unit), cursorAfterUnit);
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
      final CompletableFuture<Suggestions> result = new CompletableFuture<>();
      CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
          .thenRun(() -> {
            final List<Suggestions> results = new ArrayList<>();
            for (final CompletableFuture<Suggestions> future : completableFutures) {
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
  }

  public <T> @NotNull T parseAndSuggestValues(Iterable<@Nullable T> iterable, Function<@NotNull T, String> suggestions, Function<@NotNull T, @Nullable Message> tooltip, FailableFunction<String, @Nullable T, CommandSyntaxException> valueGetter) throws CommandSyntaxException {
    setSuggestion((context, builder) -> CommandSource.suggestMatching(iterable, builder, suggestions, tooltip));
    return ParsingUtil.parseValues(this.reader, valueGetter);
  }

  public <T extends Enum<T> & StringIdentifiable> @NotNull T parseAndSuggestEnums(Iterable<T> iterable, Function<@NotNull T, @Nullable Message> tooltip, FailableFunction<String, T, CommandSyntaxException> valueGetter) throws CommandSyntaxException {
    return parseAndSuggestValues(iterable, StringIdentifiable::asString, tooltip, valueGetter);
  }

  public <T extends Enum<T> & StringIdentifiable> @NotNull T parseAndSuggestEnums(Iterable<T> iterable, Function<@NotNull T, @Nullable Message> tooltip, StringIdentifiableCodec<T> codec) throws CommandSyntaxException {
    return parseAndSuggestEnums(iterable, tooltip, codec::byId);
  }

  public <T extends Enum<T> & StringIdentifiable> @NotNull T parseAndSuggestEnums(T[] iterable, Function<@NotNull T, @Nullable Message> tooltip, StringIdentifiableCodec<T> codec) throws CommandSyntaxException {
    return parseAndSuggestEnums(Arrays.asList(iterable), tooltip, codec);
  }

  /**
   * 通过指定的 {@link ArgumentType} 解析其对应的值并提供建议。调用此函数时，会确保 {@link SuggestionsBuilder} 的位置合理。
   */
  public <T> T parseAndSuggestArgument(ArgumentType<T> argumentType) throws CommandSyntaxException {
    final int cursorBeforeParse = reader.getCursor();
    setSuggestion((context, builder) -> {
      final SuggestionsBuilder builderOffset = builder.createOffset(cursorBeforeParse);
      return argumentType.listSuggestions(context, builderOffset);
    });
    return argumentType.parse(reader);
  }
}
