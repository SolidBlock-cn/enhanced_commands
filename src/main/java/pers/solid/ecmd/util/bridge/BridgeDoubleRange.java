package pers.solid.ecmd.util.bridge;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.predicate.NumberRange;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @see org.apache.commons.lang3.DoubleRange
 * @see net.minecraft.predicate.NumberRange.DoubleRange
 */
public class BridgeDoubleRange extends AbstractBridgeRange<Double> {
  public static final Codec<BridgeDoubleRange> CODEC = BridgeRange.createCodec(Codec.DOUBLE, BridgeDoubleRange::fromOptional);

  protected BridgeDoubleRange(@Nullable Double min, @Nullable Double max) {
    super(min, max);
  }

  public static BridgeDoubleRange exactly(double value) {
    return new BridgeDoubleRange(value, value);
  }

  public static BridgeDoubleRange between(double min, double max) {
    return new BridgeDoubleRange(min, max);
  }

  public static BridgeDoubleRange atLeast(double value) {
    return new BridgeDoubleRange(value, null);
  }

  public static BridgeDoubleRange atMost(double value) {
    return new BridgeDoubleRange(null, value);
  }


  private static BridgeDoubleRange fromOptional(Optional<Double> min, Optional<Double> max) {
    return new BridgeDoubleRange(min.orElse(null), max.orElse(null));
  }

  public static BridgeDoubleRange parse(StringReader reader) throws CommandSyntaxException {
    return BridgeRange.parse(reader, Double::parseDouble, (r, s) -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidDouble().createWithContext(r, s), BridgeDoubleRange::new);
  }

  public NumberRange.DoubleRange toVanilla() {
    final Optional<Double> min = Optional.ofNullable(this.min);
    final Optional<Double> max = Optional.ofNullable(this.max);
    return new NumberRange.DoubleRange(min, max, min.map(x -> x * x), max.map(x -> x * x));
  }

  @Override
  public Type getType() {
    return Type.DOUBLE;
  }
}
