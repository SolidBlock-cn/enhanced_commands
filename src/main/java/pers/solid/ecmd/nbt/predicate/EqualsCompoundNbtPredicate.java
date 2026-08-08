package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.Map;
import java.util.stream.Collectors;

public record EqualsCompoundNbtPredicate(Map<String, NbtPredicate> map) implements NbtPredicate {
  public static final MapCodec<EqualsCompoundNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(EqualsCompoundNbtPredicate::new, Codec.unboundedMap(Codec.STRING, NbtPredicate.CODEC).fieldOf("values").forGetter(EqualsCompoundNbtPredicate::map)));

  @Override
  public String expressAsString() {
    return asString(true);
  }

  @Override
  public String asString(boolean requirePrefix) {
    return (requirePrefix ? "= " : "") + "{" + map.entrySet().stream().map(entry -> {
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
  public boolean test(Tag nbtElement, ExecutionContext context) {
    if (!(nbtElement instanceof final CompoundTag nbtCompound))
      return false;
    if (nbtCompound.size() != map.size())
      return false;
    for (Map.Entry<String, NbtPredicate> entry : map.entrySet()) {
      final String key = entry.getKey();
      final NbtPredicate valuePredicate = entry.getValue();
      final Tag actualElement = nbtCompound.get(key);
      if (actualElement == null || !valuePredicate.test(actualElement, context)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public NbtPredicateType<EqualsCompoundNbtPredicate> getType() {
    return NbtPredicateTypes.EQUALS_COMPOUND;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return map.values();
  }
}
