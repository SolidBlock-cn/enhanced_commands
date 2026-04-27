package pers.solid.ecmd.nbt.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

/**
 * 无论原先值，直接返回固定时的 NBT 函数。
 *
 * @param value 使用时需要返回的值。
 */
public record SimpleNbtFunction(Tag value) implements NbtFunction {
  public static final MapCodec<SimpleNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(CodecUtil.NBT_ELEMENT.fieldOf("value").forGetter(SimpleNbtFunction::value)).apply(i, SimpleNbtFunction::new));

  @Override
  public String expressAsString() {
    return TextUtil.toSpacedStringNbt(value);
  }

  @Override
  public NbtFunctionType<SimpleNbtFunction> getType() {
    return NbtFunctionTypes.SIMPLE;
  }

  @Override
  public Tag apply(@Nullable Tag nbtElement, ExecutionContext context) {
    return value;
  }
}
