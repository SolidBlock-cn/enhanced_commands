package pers.solid.ecmd.property.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

public interface PropertyNamePredicate extends ExpressionConvertible {
  Codec<PropertyNamePredicate> CODEC = Type.CODEC.dispatch(PropertyNamePredicate::getType, type -> type.codec);

  boolean test(BlockState blockState);

  TestResult testAndDescribe(BlockState blockState, BlockPos blockPos);

  String propertyName();

  @NotNull
  Type getType();

  enum Type implements StringRepresentable {
    COMPARISON("comparison", ComparisonPropertyNamePredicate.CODEC),
    EXISTENCE("existence", ExistencePropertyNamePredicate.CODEC),
    MULTI_VALUE("multi_value", MultiValuePropertyNamePredicate.CODEC);

    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(Type.values());
    public final MapCodec<? extends PropertyNamePredicate> codec;
    private final String name;

    Type(String name, MapCodec<? extends PropertyNamePredicate> codec) {
      this.name = name;
      this.codec = codec;
    }

    @Override
    public @NotNull String getSerializedName() {
      return name;
    }
  }
}
