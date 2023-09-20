package pers.solid.ecmd.util;

import com.google.common.base.Functions;
import com.google.common.base.Suppliers;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.function.FailableSupplier;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public final class SuggestionUtil {
  private SuggestionUtil() {
  }

  public static boolean shouldSuggest(String remaining, String candidate) {
    return CommandSource.shouldSuggest(remaining, candidate);
  }

  public static <T> CompletableFuture<Suggestions> suggestMatchingWithTooltip(Iterable<T> iterable, Function<T, String> suggestion, Function<T, Message> tooltip, SuggestionsBuilder builder) {
    final String remaining = builder.getRemainingLowerCase();
    for (T t : iterable) {
      final String candidate = suggestion.apply(t);
      if (shouldSuggest(remaining, candidate)) {
        builder.suggest(candidate, tooltip.apply(t));
      }
    }
    return builder.buildFuture();
  }

  public static <T extends StringIdentifiable> CompletableFuture<Suggestions> suggestMatchingEnumWithTooltip(Iterable<T> enumIterable, Function<T, Message> tooltip, SuggestionsBuilder builder) {
    return suggestMatchingWithTooltip(enumIterable, StringIdentifiable::asString, tooltip, builder);
  }

  public static CompletableFuture<Suggestions> suggestMatchingStringWithTooltip(Iterable<String> candidates, Function<String, Message> tooltip, SuggestionsBuilder builder) {
    return suggestMatchingWithTooltip(candidates, Functions.identity(), tooltip, builder);
  }

  public static CompletableFuture<Suggestions> suggestDirections(Iterable<Direction> directions, SuggestionsBuilder builder) {
    return suggestMatchingEnumWithTooltip(directions, TextUtil::wrapDirection, builder);
  }

  public static CompletableFuture<Suggestions> suggestDirections(SuggestionsBuilder builder) {
    return suggestDirections(Arrays.asList(Direction.values()), builder);
  }

  public static void suggestString(String candidate, Supplier<Message> tooltip, SuggestionsBuilder builder) {
    if (shouldSuggest(builder.getRemainingLowerCase(), candidate.toLowerCase())) {
      builder.suggest(candidate, tooltip.get());
    }
  }

  public static void suggestString(String candidate, SuggestionsBuilder builder) {
    if (shouldSuggest(builder.getRemainingLowerCase(), candidate.toLowerCase())) {
      builder.suggest(candidate);
    }
  }

  public static void suggestString(String candidate, Message tooltip, SuggestionsBuilder builder) {
    suggestString(candidate, Suppliers.ofInstance(tooltip), builder);
  }

  /**
   * Parse and provide suggestions from an {@link ArgumentType}, and make sure all suggestions are provided correctly at a correct place.
   */
  public static <T> T suggestParserFromType(ArgumentType<T> argumentType, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final int cursorBeforeParse = parser.reader.getCursor();
    parser.suggestionProviders.add(SuggestionProvider.modifying((context, builder) -> {
      final SuggestionsBuilder builderOffset = builder.createOffset(cursorBeforeParse);
      return argumentType.listSuggestions(context, builderOffset);
    }));
    return argumentType.parse(parser.reader);
  }

  public static <T, E extends Throwable> @Nullable T parseParentheses(FailableSupplier<T, E> parseUnit, SuggestedParser parser) throws E, CommandSyntaxException {
    final StringReader reader = parser.reader;
    parser.suggestionProviders.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) suggestionsBuilder.suggest("(");
    });
    if (reader.canRead() && reader.peek() == '(') {
      reader.skip();
      reader.skipWhitespace();
      final T parse = parseUnit.get();
      parser.suggestionProviders.add((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) suggestionsBuilder.suggest(")");
      });
      reader.skipWhitespace();
      reader.expect(')');
      parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
      return parse;
    } else {
      return null;
    }
  }

  public static <T, E extends Throwable> T parseUnifiable(FailableSupplier<T, E> parseUnit, FailableFunction<List<T>, T, E> merger, String joiningString, Text joiningStringTooltip, SuggestedParser parser, boolean allowsSparse) throws E {
    final T first = parseUnit.get();
    final StringReader reader = parser.reader;
    final int cursorBeforeWhite = reader.getCursor();
    int cursorAfterLastUnit = cursorBeforeWhite;
    if (allowsSparse) reader.skipWhitespace();
    parser.suggestionProviders.add((context, suggestionsBuilder) -> suggestString(joiningString, joiningStringTooltip, suggestionsBuilder));
    if (reader.getString().startsWith(joiningString, reader.getCursor())) {
      final List<T> units = new ArrayList<>();
      units.add(first);
      while (reader.getString().startsWith(joiningString, reader.getCursor())) {
        reader.setCursor(reader.getCursor() + joiningString.length());
        parser.suggestionProviders.clear();
        if (allowsSparse) reader.skipWhitespace();
        units.add(parseUnit.get());
        cursorAfterLastUnit = reader.getCursor();
        if (allowsSparse) reader.skipWhitespace();
      }
      reader.setCursor(cursorAfterLastUnit);
      return merger.apply(units);
    } else {
      reader.setCursor(cursorBeforeWhite);
      return first;
    }
  }

  public static double parseAngle(SuggestedParser parser) throws CommandSyntaxException {
    final StringReader reader = parser.reader;
    final int cursorBeforeDouble = reader.getCursor();
    while (reader.canRead()) {
      final char peek = reader.peek();
      if ((peek < '0' || peek > '9') && peek != '-') {
        if (peek != '.') {
          break; // 无效字符
        } else if (reader.canRead(2) && reader.peek(1) != '.') {
          break; // 后面两个字符都是小数点，无效
        }
      }
      reader.skip();
    }
    final String substring = reader.getString().substring(cursorBeforeDouble, reader.getCursor());
    if (substring.isEmpty()) {
      parser.reader.setCursor(cursorBeforeDouble);
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedDouble().createWithContext(reader);
    }
    final double v;
    try {
      v = Double.parseDouble(substring);
    } catch (NumberFormatException e) {
      parser.reader.setCursor(cursorBeforeDouble);
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidDouble().createWithContext(reader, substring);
    }
    parser.suggestionProviders.add((context, suggestionsBuilder) -> CommandSource.suggestMatching(List.of("deg", "rad", "turn"), suggestionsBuilder));
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
      return Math.toRadians(v);
    } else if ("rad".equals(unit)) {
      return v;
    } else if ("turn".equals(unit)) {
      return Math.PI * 2 * v;
    } else {
      reader.setCursor(cursorBeforeUnit);
      throw ModCommandExceptionTypes.ANGLE_UNIT_UNKNOWN.createWithContext(reader, unit);
    }
  }
}
