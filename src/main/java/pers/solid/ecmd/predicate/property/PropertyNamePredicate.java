package pers.solid.ecmd.predicate.property;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.ExpressionConvertible;

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
    MULTI_VALUE("multi_value", MultiValuePropertyNamePredicate.CODEC),
    CUSTOM("custom", com.mojang.serialization.Codec.unit(null));

    private final String name;
    public final com.mojang.serialization.Codec<? extends PropertyNamePredicate> codec;
    public static final com.mojang.serialization.Codec<Type> CODEC = StringIdentifiable.createCodec(Type::values);

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
