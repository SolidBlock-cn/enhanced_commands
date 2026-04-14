package pers.solid.ecmd.nbt.predicate;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.function.Predicate;

public interface NbtPredicate extends ExpressionConvertible, Predicate<Tag> {
  Codec<NbtPredicate> CODEC = NbtPredicateType.REGISTRY.byNameCodec().dispatch(NbtPredicate::getType, NbtPredicateType::getCodec);
  ResourceKey<Registry<NbtPredicate>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("nbt_predicate"));

  static NbtPredicate parse(CommandBuildContext commandBuildContext, String s, CommandSourceStack source) throws CommandSyntaxException {
    return new NbtPredicateParser<>(new ParseContext<>(commandBuildContext, new StringReader(s), false, true)).parseNbtPredicate(false, false);
  }

  static <S> NbtPredicate parse(ParseContext<S> parseContext, boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    final NbtPredicateParser<S> n = new NbtPredicateParser<>(parseContext);
    return n.parseNbtPredicate(mustExpectSign, equalsForDefault);
  }

  @Override
  String asString();

  default String asString(boolean requirePrefix) {
    return asString();
  }

  @Override
  boolean test(Tag nbtElement);

  NbtPredicateType<?> getType();

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
    public String getSerializedName() {
      return name;
    }

    public com.mojang.serialization.Codec<? extends NbtPredicate> getCodec() {
      throw new UnsupportedOperationException();
    }
  }
}
