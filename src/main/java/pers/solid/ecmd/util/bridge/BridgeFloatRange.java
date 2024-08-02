package pers.solid.ecmd.util.bridge;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.command.FloatRangeArgument;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @see FloatRangeArgument
 */
public class BridgeFloatRange extends AbstractBridgeRange<Float> {
  public static final Codec<BridgeFloatRange> CODEC = BridgeRange.createCodec(Codec.FLOAT, BridgeFloatRange::fromOptional);

  protected BridgeFloatRange(@Nullable Float min, @Nullable Float max) {
    super(min, max);
  }

  public static BridgeFloatRange exactly(float value) {
    return new BridgeFloatRange(value, value);
  }

  public static BridgeFloatRange between(float min, float max) {
    return new BridgeFloatRange(min, max);
  }

  public static BridgeFloatRange atLeast(float value) {
    return new BridgeFloatRange(value, null);
  }

  public static BridgeFloatRange atMost(float value) {
    return new BridgeFloatRange(null, value);
  }


  private static BridgeFloatRange fromOptional(Optional<Float> min, Optional<Float> max) {
    return new BridgeFloatRange(min.orElse(null), max.orElse(null));
  }

  public static BridgeFloatRange parse(StringReader reader) throws CommandSyntaxException {
    return BridgeRange.parse(reader, Float::parseFloat, (r, s) -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidFloat().createWithContext(r, s), BridgeFloatRange::new);
  }

  public static BridgeFloatRange fromVanilla(FloatRangeArgument range) {
    return new BridgeFloatRange(range.min(), range.max());
  }

  public FloatRangeArgument toVanilla() {
    return new FloatRangeArgument(min, max);
  }
}
