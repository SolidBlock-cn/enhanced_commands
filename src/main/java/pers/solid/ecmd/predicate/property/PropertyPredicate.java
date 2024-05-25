package pers.solid.ecmd.predicate.property;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.function.Function;

public interface PropertyPredicate<T extends Comparable<T>> extends ExpressionConvertible {
  static Codec<PropertyPredicate<?>> getCodec(Block block) {
    return Type.CODEC.dispatch(PropertyPredicate::getType, type -> type.getCodec(block));
  }

  @NotNull
  static <T extends Comparable<T>> MutableText propertyAndValue(BlockState blockState, Property<T> property) {
    return Text.literal(property.getName() + "=" + property.name(blockState.get(property)));
  }

  @NotNull
  Type getType();

  boolean test(BlockState blockState);

  TestResult testAndDescribe(BlockState blockState, BlockPos blockPos);

  Property<T> property();

  enum Type implements StringIdentifiable {
    COMPARISON("comparison", ComparisonPropertyPredicate::getCodec),
    EXISTENCE("existence", ExistencePropertyPredicate::getCodec),
    MULTI_VALUE("multi_value", MultiValuePropertyPredicate::getCodec);

    public static final com.mojang.serialization.Codec<Type> CODEC = StringIdentifiable.createCodec(Type::values);
    public final Function<Block, com.mojang.serialization.Codec<? extends PropertyPredicate<?>>> codecFunction;
    private final String name;

    Type(String name, Function<Block, com.mojang.serialization.Codec<? extends PropertyPredicate<?>>> codecFunction) {
      this.name = name;
      this.codecFunction = codecFunction;
    }

    @Override
    public String asString() {
      return name;
    }

    public com.mojang.serialization.Codec<? extends PropertyPredicate<?>> getCodec(Block block) {
      return codecFunction.apply(block);
    }
  }
}
