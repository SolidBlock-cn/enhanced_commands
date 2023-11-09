package pers.solid.ecmd.argument;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.SuggestionProvider;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * <p>此类用于在解析一段内容的同时提供建议，相当于一次性完成 {@link ArgumentType#parse(StringReader)} 和 {@link ArgumentType#listSuggestions(CommandContext, SuggestionsBuilder)} 的两个工作，但需要注意的是，对于 {@link ArgumentType} 而言，这个解析过程仍会运行两遍，一遍用于解析结果，一遍用于提供建议。
 * <p>为了更灵活地控制建议提供过程，此类允许一次性提供多个建议。解析过程结束时（包括抛出 {@link CommandSyntaxException} 时），{@link #reader} 所在的 {@link StringReader#cursor cursor} 就是建议的起始位置，而这是位置也正是 {@link CommandSyntaxException} 的 cursor 位置。如果由于某些原因必须指定命令建议的起始位置，可以使用 {@link SuggestionProvider#offset(SuggestionProvider.Offset)} 作为建议内容。
 */
public class SuggestedParser {
  public static final DynamicCommandExceptionType UNKNOWN_VALUE = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.argument.unknown_value", o));
  /**
   * 此对象的基于的 {@link StringReader} 对象，会直接用于解析。在提供建议时，也会基于此对象的 {@link StringReader#string string} 和 {@link StringReader#cursor cursor} 来提供建议。
   */
  public final StringReader reader;
  /**
   * 在当前解析过程中所需要提供的建议。解析的过程不提供具体的建议，只指定如何提供建议（{@link SuggestionProvider}）。可以提供多种不同的建议。
   */
  public final List<SuggestionProvider> suggestionProviders;

  public SuggestedParser(String string) {
    this(new StringReader(string));
  }

  public SuggestedParser(StringReader reader) {
    this(reader, new ArrayList<>());
  }

  public SuggestedParser(StringReader reader, List<SuggestionProvider> suggestionProviders) {
    this.reader = reader;
    this.suggestionProviders = suggestionProviders;
  }

  /**
   * 解析整数的向量。这不是代表一个坐标，因此也不支持绝对坐标和局部坐标。
   */
  public Function<ServerCommandSource, Vec3i> parseAndSuggestVec3i() throws CommandSyntaxException {
    final StringReader reader = this.reader;
    {
      suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = this.reader.readUnquotedString();
      final DirectionArgument byName = DirectionArgument.CODEC.byId(unquotedString);
      if (byName != null) {
        suggestionProviders.remove(suggestionProviders.size() - 1);
        return source -> byName.apply(source).getVector();
      } else {
        this.reader.setCursor(cursorBeforeDirection);
      }
    }
    final int x = reader.readInt();
    suggestionProviders.remove(suggestionProviders.size() - 1);
    ParsingUtil.expectAndSkipWhitespace(reader);
    {
      suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = this.reader.readUnquotedString();
      final DirectionArgument byName = DirectionArgument.CODEC.byId(unquotedString);
      if (byName != null) {
        suggestionProviders.remove(suggestionProviders.size() - 1);
        return source -> byName.apply(source).getVector().multiply(x);
      } else {
        this.reader.setCursor(cursorBeforeDirection);
      }
    }
    final int y = reader.readInt();
    suggestionProviders.remove(suggestionProviders.size() - 1);
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
      suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = reader.readUnquotedString();
      final DirectionArgument byName = DirectionArgument.CODEC.byId(unquotedString);
      if (byName != null) {
        suggestionProviders.remove(suggestionProviders.size() - 1);
        return source -> Vec3d.of(byName.apply(source).getVector());
      } else {
        reader.setCursor(cursorBeforeDirection);
      }
    }
    final double x = reader.readDouble();
    suggestionProviders.remove(suggestionProviders.size() - 1);
    ParsingUtil.expectAndSkipWhitespace(reader);
    {
      suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestDirections(suggestionsBuilder));
      final int cursorBeforeDirection = reader.getCursor();
      final String unquotedString = reader.readUnquotedString();
      final DirectionArgument byName = DirectionArgument.CODEC.byId(unquotedString);
      if (byName != null) {
        suggestionProviders.remove(suggestionProviders.size() - 1);
        return source -> Vec3d.of(byName.apply(source).getVector()).multiply(x);
      } else {
        reader.setCursor(cursorBeforeDirection);
      }
    }
    final double y = reader.readDouble();
    suggestionProviders.remove(suggestionProviders.size() - 1);
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
    suggestionProviders.add((context, suggestionsBuilder) -> CommandSource.suggestMatching(List.of("deg", "rad", "turn"), suggestionsBuilder));
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
      suggestionProviders.remove(suggestionProviders.size() - 1);
      return radians ? Math.toRadians(v) : v;
    } else if ("rad".equals(unit)) {
      suggestionProviders.remove(suggestionProviders.size() - 1);
      return radians ? v : Math.toDegrees(v);
    } else if ("turn".equals(unit)) {
      suggestionProviders.remove(suggestionProviders.size() - 1);
      return (radians ? Math.PI * 2 : 360) * v;
    } else {
      final int cursorAfterUnit = reader.getCursor();
      reader.setCursor(cursorBeforeUnit);
      throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.ANGLE_UNIT_UNKNOWN.createWithContext(reader, unit), cursorAfterUnit);
    }
  }

  /**
   * 根据 {@link #suggestionProviders} 中的内容提供建议，并会进行合并。
   *
   * @see com.mojang.brigadier.CommandDispatcher#getCompletionSuggestions(ParseResults, int)
   */
  public CompletableFuture<Suggestions> buildSuggestions(CommandContext<?> context, SuggestionsBuilder builder) {
    if (suggestionProviders.isEmpty()) {
      return Suggestions.empty();
    }
    final List<CompletableFuture<Suggestions>> completableFutures = new ArrayList<>();
    for (SuggestionProvider suggestionProvider : suggestionProviders) {
      if (suggestionProvider instanceof SuggestionProvider.Offset offset) {
        final CompletableFuture<Suggestions> future = offset.apply(context, builder);
        completableFutures.add(future);
      } else {
        suggestionProvider.accept(context, builder);
      }
    }
    final CompletableFuture<Suggestions> directFuture = builder.buildFuture();
    if (completableFutures.isEmpty()) {
      return directFuture;
    } else {
      completableFutures.add(directFuture);
      final CompletableFuture<Suggestions> result = new CompletableFuture<>();
      CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
          .thenRun(() -> {
            final List<Suggestions> suggestions = new ArrayList<>();
            for (final CompletableFuture<Suggestions> future : completableFutures) {
              suggestions.add(future.join());
            }
            result.complete(Suggestions.merge(reader.getString(), suggestions));
          });
      return result;
    }
  }

  public <T> @NotNull T parseAndSuggestValues(Iterable<@Nullable T> iterable, Function<T, String> suggestions, Function<T, @Nullable Message> tooltip, FailableFunction<String, T, CommandSyntaxException> valueGetter) throws CommandSyntaxException {
    final int cursorBeforeRead = reader.getCursor();
    this.suggestionProviders.add((context, builder) -> ParsingUtil.suggestMatchingWithTooltip(iterable, suggestions, tooltip, builder));
    final String name = reader.readString();
    final T value = valueGetter.apply(name);
    if (value == null) {
      reader.setCursor(cursorBeforeRead);
      throw UNKNOWN_VALUE.createWithContext(reader, name);
    } else {
      return value;
    }
  }

  public <T extends Enum<T> & StringIdentifiable> @NotNull T parseAndSuggestEnums(Iterable<T> iterable, Function<T, @Nullable Message> tooltip, FailableFunction<String, T, CommandSyntaxException> valueGetter) throws CommandSyntaxException {
    return parseAndSuggestValues(iterable, StringIdentifiable::asString, tooltip, valueGetter);
  }

  public <T extends Enum<T> & StringIdentifiable> @NotNull T parseAndSuggestEnums(Iterable<T> iterable, Function<T, @Nullable Message> tooltip, StringIdentifiable.Codec<T> codec) throws CommandSyntaxException {
    return parseAndSuggestEnums(iterable, tooltip, codec::byId);
  }

  public <T extends Enum<T> & StringIdentifiable> @NotNull T parseAndSuggestEnums(T[] iterable, Function<T, @Nullable Message> tooltip, StringIdentifiable.Codec<T> codec) throws CommandSyntaxException {
    return parseAndSuggestEnums(Arrays.asList(iterable), tooltip, codec);
  }

  /**
   * 通过指定的 {@link ArgumentType} 解析其对应的值并提供建议。调用此函数时，会确保 {@link SuggestionsBuilder} 的位置合理。
   */
  public <T> T parseAndSuggestArgument(ArgumentType<T> argumentType) throws CommandSyntaxException {
    final int cursorBeforeParse = reader.getCursor();
    suggestionProviders.add(SuggestionProvider.offset((context, builder) -> {
      final SuggestionsBuilder builderOffset = builder.createOffset(cursorBeforeParse);
      return argumentType.listSuggestions(context, builderOffset);
    }));
    return argumentType.parse(reader);
  }
}
