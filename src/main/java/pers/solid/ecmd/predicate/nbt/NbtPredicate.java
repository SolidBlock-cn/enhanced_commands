package pers.solid.ecmd.predicate.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.NbtPredicateSuggestedParser;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.function.Predicate;

public interface NbtPredicate extends ExpressionConvertible, Predicate<@NotNull NbtElement> {
  @Override
  @NotNull String asString();

  default @NotNull String asString(boolean requirePrefix) {
    return asString();
  }

  @Override
  boolean test(@NotNull NbtElement nbtElement);

  @NotNull
  Type getType();

  //  Codec<NbtPredicate> CODEC = Type.CODEC.dispatch(NbtPredicate::getType, Type::getCodec);
  Codec<NbtPredicate> CODEC = Codec.STRING.flatXmap(s -> {
    try {
      return DataResult.success(new NbtPredicateSuggestedParser(new StringReader(s)).parsePredicate(false, false));
    } catch (CommandSyntaxException e) {
      return DataResult.error(e::getMessage);
    }
  }, nbtPredicate -> DataResult.success(nbtPredicate.asString()));

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

    private final String name;
    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(Type.values());

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
