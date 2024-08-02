package pers.solid.ecmd.function.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

public interface PropertyNameFunction extends ExpressionConvertible {
  Codec<PropertyNameFunction> CODEC = Type.CODEC.dispatch(PropertyNameFunction::getType, type -> type.codec);

  /**
   * 当 must 为 true 时，返回属性或者抛出异常。当 must 为 false 时，返回属性或者 null，不抛出异常。
   */
  @Nullable
  static Property<?> getProperty(@NotNull BlockState blockState, String propertyName, boolean must) {
    final StateManager<Block, BlockState> stateManager = blockState.getBlock().getStateManager();
    final Property<?> property = stateManager.getProperty(propertyName);
    if (property == null || !blockState.contains(property)) {
      if (must) {
        throw new IllegalArgumentException("property propertyName");
      } else {
        return null;
      }
    }
    return property;
  }

  @Contract(pure = true)
  BlockState getModifiedState(BlockState origState, BlockState blockState, Random random);

  @Contract(pure = true)
  String propertyName();

  @NotNull
  Type getType();

  enum Type implements StringIdentifiable {
    ALL_ORIGINAL("all_original", AllOriginalPropertyNameFunctions.CODEC),
    ALL_RANDOM("all_random", AllRandomPropertyNameFunction.CODEC),
    BYPASSING("bypassing", BypassingPropertyNameFunction.CODEC),
    RANDOM("random", RandomPropertyNameFunction.CODEC),
    SIMPLE("simple", SimplePropertyNameFunction.CODEC);
    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(Type.values());

    private final String name;
    private final MapCodec<? extends PropertyNameFunction> codec;

    Type(String name, MapCodec<? extends PropertyNameFunction> codec) {
      this.name = name;
      this.codec = codec;
    }

    @Override
    public String asString() {
      return name;
    }
  }
}
