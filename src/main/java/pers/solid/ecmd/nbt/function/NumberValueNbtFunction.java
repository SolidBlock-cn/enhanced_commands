package pers.solid.ecmd.nbt.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.codec.CodecUtil;

/**
 * 将一个 NBT 数字的数值更改为另一个数值，但不改变原有元素的数据类型。如果原 NBT 元素不是数字的类型，那么直接修改。例如：
 * <pre>
 *   2b(3) = 2
 *   2s(3l) = 2l
 *   2s("other type") = 2s
 *   2s(null) = 2s
 * </pre>
 */
public record NumberValueNbtFunction(NumericTag value) implements NbtFunction {
  public static final MapCodec<NumberValueNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(CodecUtil.NBT_NUMBER.fieldOf("value").forGetter(NumberValueNbtFunction::value)).apply(i, NumberValueNbtFunction::new));

  @Override
  public String expressAsString() {
    return expressAsString(true);
  }

  @Override
  public String expressAsString(boolean requirePrefix) {
    return "= " + value;
  }

  @Override
  public NbtFunctionType<NumberValueNbtFunction> getType() {
    return NbtFunctionTypes.NUMBER_VALUE;
  }

  @Override
  public Tag apply(@Nullable Tag nbtElement, ExecutionContext context) {
    if (nbtElement instanceof DoubleTag) {
      return DoubleTag.valueOf(value.getAsDouble());
    } else if (nbtElement instanceof FloatTag) {
      return FloatTag.valueOf(value.getAsFloat());
    } else if (nbtElement instanceof LongTag) {
      return LongTag.valueOf(value.getAsLong());
    } else if (nbtElement instanceof IntTag) {
      return IntTag.valueOf(value.getAsInt());
    } else if (nbtElement instanceof ShortTag) {
      return ShortTag.valueOf(value.getAsShort());
    } else {
      return value;
    }
  }
}
