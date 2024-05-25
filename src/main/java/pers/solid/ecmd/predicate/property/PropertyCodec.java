package pers.solid.ecmd.predicate.property;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;

public enum PropertyCodec implements Codec<Property<?>> {
  INSTANCE;

  @Override
  public <T> DataResult<Pair<Property<?>, T>> decode(DynamicOps<T> ops, T input) {
    final DataResult<String> stringValue = ops.getStringValue(input);
    if (ops instanceof BlockBiasedOps<T> blockBiasedOps) {
      final StateManager<Block, BlockState> stateManager = blockBiasedOps.getStateManager();
      return stringValue.flatMap(s -> {
        final Property<?> property = stateManager.getProperty(s);
        if (property == null) {
          return DataResult.error(() -> stateManager.getOwner() + " does not support property named " + s);
        }
        return DataResult.success(Pair.of(property, ops.empty()));
      });
    }
    return DataResult.error(() -> "The ops is not instance of " + BlockBiasedOps.class + ", and cannot get property from propertyName!");
  }

  @Override
  public <T> DataResult<T> encode(Property<?> input, DynamicOps<T> ops, T prefix) {
    return DataResult.success(ops.createString(input.getName()));
  }
}
