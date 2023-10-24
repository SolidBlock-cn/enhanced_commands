package pers.solid.ecmd.argument;

import com.google.common.collect.ImmutableCollection;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.EnumOrRandom;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SimpleEnumArgumentType<E extends Enum<E>> implements ArgumentType<E> {
  private final ImmutableCollection<@NotNull E> values;
  private final Function<@NotNull E, @NotNull String> toString;
  private final Function<@NotNull String, @Nullable E> fromString;
  private final Function<@NotNull E, @Nullable Message> tooltip;

  public SimpleEnumArgumentType(ImmutableCollection<@NotNull E> values, Function<@NotNull E, @NotNull String> toString, Function<@NotNull String, @Nullable E> fromString, Function<@NotNull E, @Nullable Message> tooltip) {
    this.values = values;
    this.toString = toString;
    this.fromString = fromString;
    this.tooltip = tooltip;
  }

  public static <E extends Enum<E> & StringIdentifiable> SimpleEnumArgumentType<E> createStringIdentifiable(ImmutableCollection<E> values, Codec<E> codec, Function<@NotNull E, @Nullable Message> tooltip) {
    return new SimpleEnumArgumentType<>(values, StringIdentifiable::asString, string -> codec.parse(JsonOps.INSTANCE, new JsonPrimitive(string)).result().orElse(null), tooltip);
  }

  @Override
  public E parse(StringReader reader) throws CommandSyntaxException {
    final int cursorBeforeEnum = reader.getCursor();
    final String unquotedString = reader.readUnquotedString();
    final E apply = fromString.apply(unquotedString);
    if (apply == null) {
      final int cursorAfterEnum = reader.getCursor();
      reader.setCursor(cursorBeforeEnum);
      throw CommandSyntaxExceptionExtension.withCursorEnd(EnumOrRandom.INVALID_ENUM_EXCEPTION.createWithContext(reader, unquotedString), cursorAfterEnum);
    }
    return apply;
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return ParsingUtil.suggestMatchingWithTooltip(values, toString, tooltip, builder);
  }

  @Override
  public Collection<String> getExamples() {
    return values.stream().limit(3).map(toString).toList();
  }
}
