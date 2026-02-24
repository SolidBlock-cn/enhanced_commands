package pers.solid.ecmd.math;

import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.ParseContext;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface EnumOrRandom<E extends Enum<E> & StringRepresentable> extends StringRepresentable, Function<RandomSource, E> {
  DynamicCommandExceptionType INVALID_ENUM_EXCEPTION = new DynamicCommandExceptionType(
      value -> Component.translatable("argument.enum.invalid", value)
  );

  static <E extends Enum<E> & StringRepresentable> Instance<E> of(E value) {
    return new Instance<>(value);
  }

  static <E extends Enum<E> & StringRepresentable> RandomValue<E> random(E[] values) {
    return new RandomValue<>(values, "*");
  }

  static <E extends Enum<E> & StringRepresentable> Optional<EnumOrRandom<E>> parse(com.mojang.serialization.Codec<E> codec, String s, Supplier<E[]> randomizedSupplier) {
    return "random".equals(s) ? Optional.of(random(randomizedSupplier.get())) : codec.parse(JsonOps.INSTANCE, new JsonPrimitive(s)).result().map(EnumOrRandom::of);
  }

  /**
   * @see net.minecraft.commands.arguments.StringRepresentableArgument#parse(StringReader)
   */
  @SuppressWarnings("deprecation")
  static <E extends Enum<E> & StringRepresentable> EnumOrRandom<E> parseAndSuggest(E[] values, Codec<E> codec, ParseContext<?> parseContext) throws CommandSyntaxException {
    parseContext.addSuggestion((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("*");
      }
      SharedSuggestionProvider.suggest(Arrays.stream(values).map(StringRepresentable::getSerializedName), suggestionsBuilder);
      return suggestionsBuilder.buildFuture();
    });
    final StringReader reader = parseContext.reader();
    if (reader.canRead() && reader.peek() == '*') {
      reader.skip();
      return random(values);
    } else {
      final int cursorBeforeParse = reader.getCursor();
      final String s = reader.readUnquotedString();
      Optional<E> optional = codec instanceof EnumCodec<E> codec1 ? Optional.ofNullable(codec1.byName(s)) : codec.parse(JsonOps.INSTANCE, new JsonPrimitive(s)).result();
      return of(optional.orElseThrow(() -> {
        reader.setCursor(cursorBeforeParse);
        return INVALID_ENUM_EXCEPTION.createWithContext(reader, s);
      }));
    }
  }

  static <E extends Enum<E> & StringRepresentable> com.mojang.serialization.Codec<EnumOrRandom<E>> getCodec(com.mojang.serialization.Codec<E> codec, Supplier<E[]> values) {
    return com.mojang.serialization.Codec.either(
        com.mojang.serialization.Codec.STRING.flatXmap(s -> "*".equals(s) ? DataResult.success(random(values.get())) : DataResult.error(() -> "not random value"), v -> DataResult.success("*")),
        codec
    ).xmap(either -> either.map(Function.identity(), EnumOrRandom::of), o -> o instanceof EnumOrRandom.Instance<E> i ? Either.right(i.value) : Either.left(((RandomValue<E>) o)));
  }

  record Instance<E extends Enum<E> & StringRepresentable>(E value) implements EnumOrRandom<E> {

    @Override
    public @NotNull String getSerializedName() {
      return value.getSerializedName();
    }

    @Override
    public E apply(RandomSource random) {
      return value;
    }
  }

  record RandomValue<E extends Enum<E> & StringRepresentable>(E[] values, String name) implements EnumOrRandom<E> {
    @Override
    public @NotNull String getSerializedName() {
      return name;
    }

    @Override
    public E apply(RandomSource random) {
      return values[random.nextInt(values.length)];
    }
  }
}
