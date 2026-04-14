package pers.solid.ecmd.property.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.function.Function;

public interface PropertyPredicate<T extends Comparable<T>> extends ExpressionConvertible {
  static Codec<PropertyPredicate<?>> getCodec(Block block) {
    return Type.CODEC.dispatch(PropertyPredicate::getType, type -> type.getCodec(block));
  }

  static <T extends Comparable<T>> MutableComponent propertyAndValue(BlockState blockState, Property<T> property) {
    return Component.literal(property.getName() + "=" + property.getName(blockState.getValue(property)));
  }

  Type getType();

  boolean test(BlockState blockState);

  TestResult testAndDescribe(BlockState blockState, BlockPos blockPos);

  Property<T> property();

  enum Type implements StringRepresentable {
    COMPARISON("comparison", ComparisonPropertyPredicate::getCodec),
    EXISTENCE("existence", ExistencePropertyPredicate::getCodec),
    MULTI_VALUE("multi_value", MultiValuePropertyPredicate::getCodec);

    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(Type.values());
    public final Function<Block, MapCodec<? extends PropertyPredicate<?>>> codecFunction;
    private final String name;

    Type(String name, Function<Block, MapCodec<? extends PropertyPredicate<?>>> codecFunction) {
      this.name = name;
      this.codecFunction = codecFunction;
    }

    @Override
    public String getSerializedName() {
      return name;
    }

    public MapCodec<? extends PropertyPredicate<?>> getCodec(Block block) {
      return codecFunction.apply(block);
    }
  }
}
