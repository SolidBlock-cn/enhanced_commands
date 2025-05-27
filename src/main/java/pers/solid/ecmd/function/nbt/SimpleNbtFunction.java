package pers.solid.ecmd.function.nbt;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

/**
 * 无论原先值，直接返回固定时的 NBT 函数。
 *
 * @param element 使用时需要返回的值。
 */
public record SimpleNbtFunction(@NotNull NbtElement element) implements NbtFunction {
  public static final MapCodec<SimpleNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(CodecUtil.NBT_ELEMENT.fieldOf("element").forGetter(SimpleNbtFunction::element)).apply(i, SimpleNbtFunction::new));

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (requirePrefix ? ": " : "") + TextUtil.toSpacedStringNbt(element);
  }

  @Override
  public NbtFunctionType<SimpleNbtFunction> getType() {
    return Type.SIMPLE_TYPE;
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement) {
    return element;
  }

  public enum Type implements NbtFunctionType<SimpleNbtFunction> {
    SIMPLE_TYPE;

    @Override
    public MapCodec<SimpleNbtFunction> getCodec() {
      return CODEC;
    }
  }
}
