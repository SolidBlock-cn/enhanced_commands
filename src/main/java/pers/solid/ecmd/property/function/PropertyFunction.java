package pers.solid.ecmd.property.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.function.Function;

/**
 * 用于修改一个方块的方块状态属性的函数，通常用于方块函数中。通常来说，状态属性函数包含状态属性以及一个值，用于将方块的这个属性设置为另一个值。
 *
 * @param <T> 该属性的类型。
 */
public interface PropertyFunction<T extends Comparable<T>> extends ExpressionConvertible {
  static Codec<PropertyFunction<?>> getCodec(Block block) {
    return Type.CODEC.dispatch(PropertyFunction::getType, type -> type.getCodec(block));
  }

  /**
   * 修改方块状态，并返回修改后的方块状态。由于方块状态是不可变对象，因此返回的是另一个方块状态对象（也有可能是同一个）。
   *
   * @param blockState 当前正在修改的方块状态
   * @param origState  整个修改过程之前的方块状态
   */
  @Contract(pure = true)
  BlockState getModifiedState(BlockState blockState, BlockState origState, RandomSource random);

  /**
   * 该函数需要修改的那个属性，必须是准确的属性，而非根据属性的名称来匹配到那个名称的属性。
   */
  @Contract(pure = true)
  Property<T> property();

  Type getType();

  enum Type implements StringRepresentable {
    ALL_ORIGINAL("all_original", AllOriginalPropertyFunction::getCodec),
    ALL_RANDOM("all_random", AllRandomPropertyFunction::getCodec),
    BYPASSING("bypassing", BypassingPropertyFunction::getCodec),
    RANDOM("random", RandomPropertyFunction::getCodec),
    SIMPLE("simple", SimplePropertyFunction::getCodec);
    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(Type.values());

    private final String name;
    private final Function<Block, MapCodec<? extends PropertyFunction<?>>> codecFunction;

    Type(String name, Function<Block, MapCodec<? extends PropertyFunction<?>>> codecFunction) {
      this.name = name;
      this.codecFunction = codecFunction;
    }

    @Override
    public @NotNull String getSerializedName() {
      return name;
    }

    public MapCodec<? extends PropertyFunction<?>> getCodec(Block block) {
      return codecFunction.apply(block);
    }
  }
}
