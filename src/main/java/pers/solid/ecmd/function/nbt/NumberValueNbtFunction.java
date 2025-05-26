package pers.solid.ecmd.function.nbt;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 将一个 NBT 数字的数值更改为另一个数值，但不改变原有元素的数据类型。如果原 NBT 元素不是数字的类型，那么直接修改。例如：
 * <pre>
 *   2b(3) = 2
 *   2s(3l) = 2l
 *   2s("other type") = 2s
 *   2s(null) = 2s
 * </pre>
 */
public record NumberValueNbtFunction(AbstractNbtNumber number) implements NbtFunction {
  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return "= " + number.toString();
  }

  public static final MapCodec<NumberValueNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(NbtCompound.CODEC.comapFlatMap(nbtCompound -> nbtCompound.get("value") instanceof AbstractNbtNumber n ? DataResult.success(n) : DataResult.error(() -> "not a number in the NBT"), n -> {
    final NbtCompound nbtCompound = new NbtCompound();
    nbtCompound.put("value", n);
    return nbtCompound;
  }).fieldOf("number").forGetter(NumberValueNbtFunction::number)).apply(i, NumberValueNbtFunction::new));

  @Override
  public NbtFunctionType<?> getType() {
    return Type.NUMBER_VALUE_TYPE;
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement) {
    if (nbtElement instanceof NbtDouble) {
      return NbtDouble.of(number.doubleValue());
    } else if (nbtElement instanceof NbtFloat) {
      return NbtFloat.of(number.floatValue());
    } else if (nbtElement instanceof NbtLong) {
      return NbtLong.of(number.longValue());
    } else if (nbtElement instanceof NbtInt) {
      return NbtInt.of(number.intValue());
    } else if (nbtElement instanceof NbtShort) {
      return NbtShort.of(number.shortValue());
    } else {
      return number;
    }
  }

  public enum Type implements NbtFunctionType<NumberValueNbtFunction> {
    NUMBER_VALUE_TYPE;

    @Override
    public MapCodec<NumberValueNbtFunction> getCodec() {
      return CODEC;
    }
  }
}
