package pers.solid.ecmd.predicate.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Map;
import java.util.stream.Collectors;

public record EqualsCompoundNbtPredicate(@NotNull Map<@NotNull String, @NotNull NbtPredicate> map, boolean inverted) implements NbtPredicate {
  public static final MapCodec<EqualsCompoundNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(EqualsCompoundNbtPredicate::new, Codec.unboundedMap(Codec.STRING, NbtPredicate.CODEC).fieldOf("predicates").forGetter(EqualsCompoundNbtPredicate::map), Codec.BOOL.optionalFieldOf("inverted", false).forGetter(EqualsCompoundNbtPredicate::inverted)));

  @Override
  public @NotNull String asString() {
    return asString(true);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
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
  public boolean test(@NotNull NbtElement nbtElement) {
    if (!(nbtElement instanceof final NbtCompound nbtCompound))
      return inverted;
    if (nbtCompound.getSize() != map.size())
      return inverted;
    for (Map.Entry<String, NbtPredicate> entry : map.entrySet()) {
      final String key = entry.getKey();
      final NbtPredicate valuePredicate = entry.getValue();
      final NbtElement actualElement = nbtCompound.get(key);
      if (actualElement == null || !valuePredicate.test(actualElement)) {
        return inverted;
      }
    }
    return !inverted;
  }

  @Override
  public @NotNull NbtPredicateType<EqualsCompoundNbtPredicate> getType() {
    return Type.EQUALS_COMPOUND_TYPE;
  }

  public enum Type implements NbtPredicateType<EqualsCompoundNbtPredicate> {
    EQUALS_COMPOUND_TYPE;

    @Override
    public MapCodec<EqualsCompoundNbtPredicate> getCodec() {
      return CODEC;
    }
  }
}
