package pers.solid.ecmd.predicate.property;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;

public interface PropertyNamePredicate extends ExpressionConvertible {
  Codec<PropertyNamePredicate> CODEC = Type.CODEC.dispatch(PropertyNamePredicate::getType, type -> type.codec);

  boolean test(BlockState blockState);

  TestResult testAndDescribe(BlockState blockState, BlockPos blockPos);

  String propertyName();

  @NotNull
  Type getType();

  enum Type implements StringIdentifiable {
    COMPARISON("comparison", ComparisonPropertyNamePredicate.CODEC),
    EXISTENCE("existence", ExistencePropertyNamePredicate.CODEC),
    MULTI_VALUE("multi_value", MultiValuePropertyNamePredicate.CODEC);

    public static final com.mojang.serialization.Codec<Type> CODEC = StringIdentifiable.createCodec(Type::values);
    public final com.mojang.serialization.Codec<? extends PropertyNamePredicate> codec;
    private final String name;

    Type(String name, com.mojang.serialization.Codec<? extends PropertyNamePredicate> codec) {
      this.name = name;
      this.codec = codec;
    }

    @Override
    public String asString() {
      return name;
    }
  }
}
