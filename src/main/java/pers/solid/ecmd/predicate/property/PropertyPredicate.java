package pers.solid.ecmd.predicate.property;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.ExpressionConvertible;

public interface PropertyPredicate<T extends Comparable<T>> extends ExpressionConvertible {
  Codec<PropertyPredicate<?>> CODEC = Type.CODEC.dispatch(PropertyPredicate::getType, type -> type.codec);

  @NotNull
  Type getType();

  @NotNull
  static <T extends Comparable<T>> MutableText propertyAndValue(BlockState blockState, Property<T> property) {
    return Text.literal(property.getName() + "=" + property.name(blockState.get(property)));
  }

  boolean test(BlockState blockState);

  TestResult testAndDescribe(BlockState blockState, BlockPos blockPos);

  Property<T> property();

  enum Type implements StringIdentifiable {
    COMPARISON("comparison", ComparisonPropertyPredicate.CODEC),
    EXISTENCE("existence", ExistencePropertyPredicate.CODEC),
    MULTI_VALUE("multi_value", MultiValuePropertyPredicate.CODEC),
    CUSTOM("custom", com.mojang.serialization.Codec.unit(null));

    public static final com.mojang.serialization.Codec<Type> CODEC = StringIdentifiable.createCodec(Type::values);
    private final String name;
    public final com.mojang.serialization.Codec<? extends PropertyPredicate<?>> codec;

    Type(String name, com.mojang.serialization.Codec<? extends PropertyPredicate<?>> codec) {
      this.name = name;
      this.codec = codec;
    }

    @Override
    public String asString() {
      return name;
    }
  }
}
