package pers.solid.ecmd.predicate.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.NbtPredicateParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.function.Predicate;

public interface NbtPredicate extends ExpressionConvertible, Predicate<@NotNull Tag> {
  Codec<NbtPredicate> CODEC = NbtPredicateType.REGISTRY.byNameCodec().dispatch(NbtPredicate::getType, NbtPredicateType::getCodec);
  ResourceKey<Registry<NbtPredicate>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("nbt_predicate"));

  static @NotNull NbtPredicate parse(CommandBuildContext commandBuildContext, String s, CommandSourceStack source) throws CommandSyntaxException {
    return new NbtPredicateParser<>(new ParseContext<>(commandBuildContext, new StringReader(s), false, true)).parseNbtPredicate(false, false);
  }

  static <S> NbtPredicate parse(ParseContext<S> parseContext, boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    final NbtPredicateParser<S> n = new NbtPredicateParser<>(parseContext);
    return n.parseNbtPredicate(mustExpectSign, equalsForDefault);
  }

  @Override
  @NotNull String asString();

  default @NotNull String asString(boolean requirePrefix) {
    return asString();
  }

  @Override
  boolean test(@NotNull Tag nbtElement);

  @NotNull NbtPredicateType<?> getType();

  enum Type implements StringRepresentable {
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
    public @NotNull String getSerializedName() {
      return name;
    }

    public com.mojang.serialization.Codec<? extends NbtPredicate> getCodec() {
      throw new UnsupportedOperationException();
    }
  }
}
