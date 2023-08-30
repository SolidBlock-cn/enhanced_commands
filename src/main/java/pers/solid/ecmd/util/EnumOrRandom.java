package pers.solid.ecmd.util;

import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.JsonOps;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.random.Random;
import pers.solid.ecmd.argument.SuggestedParser;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public sealed interface EnumOrRandom<E extends Enum<E> & StringIdentifiable> extends StringIdentifiable, Supplier<E> {
  Random RANDOM = Random.create();
  DynamicCommandExceptionType INVALID_ENUM_EXCEPTION = new DynamicCommandExceptionType(
      value -> Text.translatable("argument.enum.invalid", value)
  );

  static <E extends Enum<E> & StringIdentifiable> Instance<E> of(E value) {
    return new Instance<>(value);
  }

  static <E extends Enum<E> & StringIdentifiable> RandomValue<E> random(E[] values) {
    return new RandomValue<>(values, "*");
  }

  static <E extends Enum<E> & StringIdentifiable> Optional<EnumOrRandom<E>> parse(com.mojang.serialization.Codec<E> codec, String s, Supplier<E[]> randomizedSupplier) {
    return "random".equals(s) ? Optional.of(random(randomizedSupplier.get())) : codec.parse(JsonOps.INSTANCE, new JsonPrimitive(s)).result().map(EnumOrRandom::of);
  }

  /**
   * @see net.minecraft.command.argument.EnumArgumentType#parse(StringReader)
   */
  static <E extends Enum<E> & StringIdentifiable> EnumOrRandom<E> parseAndSuggest(E[] values, com.mojang.serialization.Codec<E> codec, SuggestedParser parser) throws CommandSyntaxException {
    parser.suggestions.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("*");
      }
      CommandSource.suggestMatching(Arrays.stream(values).map(StringIdentifiable::asString), suggestionsBuilder);
    });
    if (parser.reader.canRead() && parser.reader.peek() == '*') {
      parser.reader.skip();
      return random(values);
    } else {
      final int cursorBeforeParse = parser.reader.getCursor();
      final String s = parser.reader.readUnquotedString();
      Optional<E> optional = codec instanceof Codec<E> codec1 ? Optional.ofNullable(codec1.byId(s)) : codec.parse(JsonOps.INSTANCE, new JsonPrimitive(s)).result();
      return of(optional.orElseThrow(() -> {
        parser.reader.setCursor(cursorBeforeParse);
        return INVALID_ENUM_EXCEPTION.createWithContext(parser.reader, s);
      }));
    }
  }

  record Instance<E extends Enum<E> & StringIdentifiable>(E value) implements EnumOrRandom<E> {
    @Override
    public E get() {
      return value;
    }

    @Override
    public String asString() {
      return value.asString();
    }
  }

  record RandomValue<E extends Enum<E> & StringIdentifiable>(E[] values, String name) implements EnumOrRandom<E> {
    @Override
    public E get() {
      return values[RANDOM.nextInt(values.length)];
    }

    @Override
    public String asString() {
      return name;
    }
  }
}
