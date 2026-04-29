package pers.solid.ecmd.property.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

public interface PropertyNameFunction extends ExpressionConvertible {
  Codec<PropertyNameFunction> CODEC = Type.CODEC.dispatch(PropertyNameFunction::getType, type -> type.codec);
  DynamicCommandExceptionType PROPERTY_DOES_NOT_EXIST = new DynamicCommandExceptionType(propertyName -> Component.translatable("enhanced_commands.property_function.property_does_not_exist", propertyName));

  /**
   * 当 must 为 true 时，返回属性或者抛出异常。当 must 为 false 时，返回属性或者 null，不抛出异常。
   */
  @Nullable
  static Property<?> getProperty(BlockState blockState, String propertyName, boolean must) throws CommandSyntaxException {
    final StateDefinition<Block, BlockState> stateManager = blockState.getBlock().getStateDefinition();
    final Property<?> property = stateManager.getProperty(propertyName);
    if (property == null || !blockState.hasProperty(property)) {
      if (must) {
        throw PROPERTY_DOES_NOT_EXIST.create(propertyName);
      } else {
        return null;
      }
    }
    return property;
  }

  @Contract(pure = true)
  BlockState getModifiedState(BlockState origState, BlockState blockState, RandomSource random) throws CommandSyntaxException;

  @Contract(pure = true)
  String propertyName();

  Type getType();

  enum Type implements StringRepresentable {
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
    public String getSerializedName() {
      return name;
    }
  }
}
