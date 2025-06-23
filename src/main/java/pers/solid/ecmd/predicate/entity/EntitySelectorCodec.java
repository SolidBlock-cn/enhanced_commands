package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.*;
import net.minecraft.command.EntitySelector;
import net.minecraft.predicate.NumberRange;

import java.util.List;
import java.util.stream.Stream;

public class EntitySelectorCodec extends MapCodec<EntitySelector> {
  private static final MapCodec<Integer> LIMIT_FIELD = Codec.INT.optionalFieldOf("limit", 0);
  private static final MapCodec<List<EntityPredicateEntry>> PREDICATE_ENTRIES = null;
  private static final MapCodec<NumberRange.DoubleRange> DISTANCE = NumberRange.DoubleRange.CODEC.optionalFieldOf("distance", NumberRange.DoubleRange.ANY);

  @Override
  public <T> Stream<T> keys(DynamicOps<T> ops) {
    return Stream.empty();
  }

  @Override
  public <T> DataResult<EntitySelector> decode(DynamicOps<T> ops, MapLike<T> input) {
    final DataResult<Integer> limit = LIMIT_FIELD.decode(ops, input);
    if (limit instanceof DataResult.Error<Integer> error) return DataResult.error(error.messageSupplier(), error.lifecycle());
    return null;
  }

  @Override
  public <T> RecordBuilder<T> encode(EntitySelector input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
    LIMIT_FIELD.encode(input.getLimit(), ops, prefix);
    return null;
  }
}
