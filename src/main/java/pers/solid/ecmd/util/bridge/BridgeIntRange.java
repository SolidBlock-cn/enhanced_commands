package pers.solid.ecmd.util.bridge;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.advancements.critereon.MinMaxBounds;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @see net.minecraft.advancements.critereon.MinMaxBounds.Ints
 * @see org.apache.commons.lang3.IntegerRange
 */
public final class BridgeIntRange extends AbstractBridgeRange<Integer> {
  public static final Codec<BridgeIntRange> CODEC = BridgeRange.createCodec(Codec.INT, BridgeIntRange::fromOptional);

  private BridgeIntRange(@Nullable Integer min, @Nullable Integer max) {
    super(min, max);
  }

  public static BridgeIntRange exactly(int value) {
    return new BridgeIntRange(value, value);
  }

  public static BridgeIntRange between(int min, int max) {
    return new BridgeIntRange(min, max);
  }

  public static BridgeIntRange atLeast(int value) {
    return new BridgeIntRange(value, null);
  }

  public static BridgeIntRange atMost(int value) {
    return new BridgeIntRange(null, value);
  }


  private static BridgeIntRange fromOptional(Optional<Integer> min, Optional<Integer> max) {
    return new BridgeIntRange(min.orElse(null), max.orElse(null));
  }

  public static BridgeIntRange parse(StringReader reader) throws CommandSyntaxException {
    return BridgeRange.parse(reader, Integer::parseInt, (r, s) -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(r, s), BridgeIntRange::new);
  }

  public static BridgeIntRange fromVanilla(MinMaxBounds.Ints range) {
    return fromOptional(range.min(), range.max());
  }

  public MinMaxBounds.Ints toVanilla() {
    final Optional<Integer> min = Optional.ofNullable(this.min);
    final Optional<Integer> max = Optional.ofNullable(this.max);
    return new MinMaxBounds.Ints(min, max, min.map(x -> x.longValue() * x.longValue()), max.map(x -> x.longValue() * x.longValue()));
  }

  @Override
  public Type getType() {
    return Type.INT;
  }
}
