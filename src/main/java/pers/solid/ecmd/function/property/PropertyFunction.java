package pers.solid.ecmd.function.property;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Contract;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.function.Function;

/**
 * 用于修改一个方块的方块状态属性的函数，通常用于方块函数中。通常来说，状态属性函数包含状态属性以及一个值，用于将方块的这个属性设置为另一个值。
 *
 * @param <T> 该属性的类型。
 */
public interface PropertyFunction<T extends Comparable<T>> extends ExpressionConvertible {
  /**
   * 修改方块状态，并返回修改后的方块状态。由于方块状态是不可变对象，因此返回的是另一个方块状态对象（也有可能是同一个）。
   *
   * @param blockState 当前正在修改的方块状态
   * @param origState  整个修改过程之前的方块状态
   */
  @Contract(pure = true)
  BlockState getModifiedState(BlockState blockState, BlockState origState, Random random);

  /**
   * 该函数需要修改的那个属性，必须是准确的属性，而非根据属性的名称来匹配到那个名称的属性。
   */
  @Contract(pure = true)
  Property<T> property();

  Type getType();

  static Codec<PropertyFunction<?>> getCodec(Block block) {
    return Type.CODEC.dispatch(PropertyFunction::getType, type -> type.getCodec(block));
  }

  enum Type implements StringIdentifiable {
    ALL_ORIGINAL("all_original", AllOriginalPropertyFunction::getCodec),
    ALL_RANDOM("all_random", AllRandomPropertyFunction::getCodec),
    BYPASSING("bypassing", BypassingPropertyFunction::getCodec),
    RANDOM("random", RandomPropertyFunction::getCodec),
    SIMPLE("simple", SimplePropertyFunction::getCodec);
    public static final com.mojang.serialization.Codec<Type> CODEC = StringIdentifiable.createCodec(Type::values);

    private final String name;
    private final Function<Block, com.mojang.serialization.Codec<? extends PropertyFunction<?>>> codecFunction;

    Type(String name, Function<Block, com.mojang.serialization.Codec<? extends PropertyFunction<?>>> codecFunction) {
      this.name = name;
      this.codecFunction = codecFunction;
    }

    @Override
    public String asString() {
      return name;
    }

    public com.mojang.serialization.Codec<? extends PropertyFunction<?>> getCodec(Block block) {
      return codecFunction.apply(block);
    }
  }
}
