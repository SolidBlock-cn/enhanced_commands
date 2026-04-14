package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.Map;
import java.util.stream.Collectors;

public record EqualsCompoundNbtPredicate(Map<String, NbtPredicate> map, boolean inverted) implements NbtPredicate {
  public static final MapCodec<EqualsCompoundNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(EqualsCompoundNbtPredicate::new, Codec.unboundedMap(Codec.STRING, NbtPredicate.CODEC).fieldOf("predicates").forGetter(EqualsCompoundNbtPredicate::map), Codec.BOOL.optionalFieldOf("inverted", false).forGetter(EqualsCompoundNbtPredicate::inverted)));

  @Override
  public String asString() {
    return asString(true);
  }

  @Override
  public String asString(boolean requirePrefix) {
    return (inverted ? "!" : "") + (requirePrefix ? "= " : "") + "{" + map.entrySet().stream().map(entry -> {
      final String key = entry.getKey();
      final String keyAsString;
      final NbtPredicate value = entry.getValue();
      keyAsString = ParsingUtil.quoteStringIfNeeded(key);
      final String valueAsString = value.asString(true);
      if (valueAsString.startsWith(":")) {
        return keyAsString + valueAsString;
      } else {
        return keyAsString + " " + valueAsString;
      }
    }).collect(Collectors.joining(", ")) + "}";
  }

  @Override
  public boolean test(Tag nbtElement) {
    if (!(nbtElement instanceof final CompoundTag nbtCompound))
      return inverted;
    if (nbtCompound.size() != map.size())
      return inverted;
    for (Map.Entry<String, NbtPredicate> entry : map.entrySet()) {
      final String key = entry.getKey();
      final NbtPredicate valuePredicate = entry.getValue();
      final Tag actualElement = nbtCompound.get(key);
      if (actualElement == null || !valuePredicate.test(actualElement)) {
        return inverted;
      }
    }
    return !inverted;
  }

  @Override
  public NbtPredicateType<EqualsCompoundNbtPredicate> getType() {
    return EqualsCompoundNbtPredicate.Type.EQUALS_COMPOUND_TYPE;
  }

  public enum Type implements NbtPredicateType<EqualsCompoundNbtPredicate> {
    EQUALS_COMPOUND_TYPE;

    @Override
    public MapCodec<EqualsCompoundNbtPredicate> getCodec() {
      return CODEC;
    }
  }
}
