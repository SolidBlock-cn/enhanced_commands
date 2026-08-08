package pers.solid.ecmd.nbt.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;
import pers.solid.ecmd.util.pack.RequiresValidation;

import java.util.function.Predicate;

public interface NbtPredicate extends ExpressionConvertible, RequiresValidation {
  Codec<NbtPredicate> CODEC = NbtPredicateType.CODEC.dispatch(NbtPredicate::getType, NbtPredicateType::codec);
  ResourceKey<Registry<NbtPredicate>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("nbt_predicate"));

  static <S> NbtPredicate parse(ParseContext<S> parseContext) throws CommandSyntaxException {
    return parse(parseContext, false, false);
  }

  static <S> NbtPredicate parse(ParseContext<S> parseContext, boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    return NbtPredicateParser.parseNbtPredicate(parseContext, mustExpectSign, equalsForDefault);
  }

  @Override
  String expressAsString();

  default String asString(boolean requirePrefix) {
    return requirePrefix ? ": " + expressAsString() : expressAsString();
  }

  boolean test(Tag nbtElement, ExecutionContext context);

  NbtPredicateType<?> getType();

  /**
   * 由于此类的 {@link #test(Tag, ExecutionContext)} 方法需要一个 {@link ExecutionContext} 对象，可以使用此方法先提供一个 {@link ExecutionContext} 使其转化为常规的 {@link Predicate}。
   */
  default Predicate<Tag> asJavaPredicate(ExecutionContext context) {
    return tag -> test(tag, context);
  }

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
