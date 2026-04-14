package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
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
public record MatchPrimitiveNbtPredicate(Tag expected) implements NbtPredicate {
  public static final MapCodec<MatchPrimitiveNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      CodecUtil.NBT_ELEMENT.fieldOf("expected").forGetter(MatchPrimitiveNbtPredicate::expected)
  ).apply(i, MatchPrimitiveNbtPredicate::new));

  @Override
  public String asString() {
    return asString(false);
  }

  @Override
  public String asString(boolean requirePrefix) {
    return (requirePrefix ? ": " : "") + TextUtil.toSpacedStringNbt(expected);
  }

  @Override
  public boolean test(Tag nbtElement) {
    return NbtUtils.compareNbt(nbtElement, expected, true);
  }

  @Override
  public NbtPredicateType<MatchPrimitiveNbtPredicate> getType() {
    return MatchPrimitiveNbtPredicate.Type.MATCH_PRIMITIVE_TYPE;
  }

  public enum Type implements NbtPredicateType<MatchPrimitiveNbtPredicate> {
    MATCH_PRIMITIVE_TYPE;

    @Override
    public MapCodec<MatchPrimitiveNbtPredicate> getCodec() {
      return CODEC;
    }
  }
}
