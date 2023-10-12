package pers.solid.ecmd.predicate.nbt;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ParsingUtil;

import java.util.Map;
import java.util.stream.Collectors;

public record EqualsCompoundNbtPredicate(@NotNull Map<@NotNull String, @NotNull NbtPredicate> map, boolean negated) implements NbtPredicate {
  @Override
  public @NotNull String asString() {
    return asString(true);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (negated ? "!" : "") + (requirePrefix ? "= " : "") + "{" + map.entrySet().stream().map(entry -> {
      final String key = entry.getKey();
      final String keyAsString;
      final NbtPredicate value = entry.getValue();
      if (ParsingUtil.isAllowedInUnquotedString(key)) {
        keyAsString = key;
      } else {
        keyAsString = NbtString.escape(key);
      }
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
      return negated;
    if (nbtCompound.getSize() != map.size())
      return negated;
    for (Map.Entry<String, NbtPredicate> entry : map.entrySet()) {
      final String key = entry.getKey();
      final NbtPredicate valuePredicate = entry.getValue();
      final NbtElement actualElement = nbtCompound.get(key);
      if (actualElement == null || !valuePredicate.test(actualElement)) {
        return negated;
      }
    }
    return !negated;
  }
}
