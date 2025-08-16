package pers.solid.ecmd.predicate.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.NbtPredicateParser;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;
import pers.solid.ecmd.parse.ParseContext;

import java.util.function.Predicate;

public interface NbtPredicate extends ExpressionConvertible, Predicate<@NotNull NbtElement> {
  Codec<NbtPredicate> CODEC = NbtPredicateType.REGISTRY.getCodec().dispatch(NbtPredicate::getType, NbtPredicateType::getCodec);

  static @NotNull NbtPredicate parse(CommandRegistryAccess registryAccess, String s, ServerCommandSource source) throws CommandSyntaxException {
    return new NbtPredicateParser<>(new ParseContext<>(registryAccess, new StringReader(s), false, true)).parsePredicate(false, false);
  }

  static <S> NbtPredicate parse(ParseContext<S> parseContext, boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    final NbtPredicateParser<S> n = new NbtPredicateParser<>(parseContext);
    return n.parsePredicate(mustExpectSign, equalsForDefault);
  }

  @Override
  @NotNull String asString();

  default @NotNull String asString(boolean requirePrefix) {
    return asString();
  }

  @Override
  boolean test(@NotNull NbtElement nbtElement);

  @NotNull NbtPredicateType<?> getType();

  enum Type implements StringIdentifiable {
    COMPARISON("comparison"),
    CONSTANT("constant"),
    EQUALS_COMPOUND("equals_compound"),
    EQUALS_LIST("equals_list"),
    MATCH_COMPOUND("match_compound"),
    MATCH_LIST("match_list"),
    MATCH_PRIMITIVE("match_primitive"),
    RANGE("range"),
    REGEX("regex");

    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(Type.values());
    private final String name;

    Type(String name) {
      this.name = name;
    }

    @Override
    public String asString() {
      return name;
    }

    public com.mojang.serialization.Codec<? extends NbtPredicate> getCodec() {
      throw new UnsupportedOperationException();
    }
  }
}
