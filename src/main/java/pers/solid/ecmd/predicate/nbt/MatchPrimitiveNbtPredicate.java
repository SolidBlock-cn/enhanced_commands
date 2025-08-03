package pers.solid.ecmd.predicate.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

/**
 * 匹配一个 NBT 的值（除了列表等复杂类型）是否能够与一个值直接匹配，通常来说要求值相等，包括内容也是相等的。例如：
 * <pre>
 *   3b match 3b -> true
 *   3b match 2b -> false
 *   3b match 3 -> false
 * </pre>
 */
public record MatchPrimitiveNbtPredicate(NbtElement expected, boolean inverted) implements NbtPredicate {
  public static final MapCodec<MatchPrimitiveNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      CodecUtil.NBT_ELEMENT.fieldOf("expected").forGetter(MatchPrimitiveNbtPredicate::expected),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(MatchPrimitiveNbtPredicate::inverted)
  ).apply(i, MatchPrimitiveNbtPredicate::new));

  @Override
  public @NotNull String asString() {
    return asString(false);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (inverted ? "!" : "") + (requirePrefix ? ": " : "") + TextUtil.toSpacedStringNbt(expected);
  }

  @Override
  public boolean test(@NotNull NbtElement nbtElement) {
    return NbtHelper.matches(nbtElement, expected, true) != inverted;
  }

  @Override
  public @NotNull NbtPredicateType<MatchPrimitiveNbtPredicate> getType() {
    return Type.MATCH_PRIMITIVE_TYPE;
  }

  public enum Type implements NbtPredicateType<MatchPrimitiveNbtPredicate> {
    MATCH_PRIMITIVE_TYPE;

    @Override
    public MapCodec<MatchPrimitiveNbtPredicate> getCodec() {
      return CODEC;
    }
  }
}
