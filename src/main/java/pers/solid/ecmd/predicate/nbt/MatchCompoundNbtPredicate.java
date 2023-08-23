package pers.solid.ecmd.predicate.nbt;

import com.google.common.collect.ListMultimap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.StringUtil;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 匹配复合标签谓词类似于原版的复合标签匹配。当实际值包含了所有的预期值时，返回 true。当实际值不为 NbtCompound 时，返回 false。其中，预期值中的键可以不指定，如果不指定，则表示无论是什么键，只要有这样一个值，就是 true。
 * <pre>
 *   {a: one, b: two} match {a: one} -> true
 *   {a: one, b: two} match {a: three} -> false
 *   {a: one, b: two} match {a: one, b: one} -> false
 *   {a: one, b: two} match {a: *} -> true
 *   {a: one, b: two} match {c: *} -> true
 *   {a: one, b: two} match {*: one} -> true
 *   {a: one, b: two} match {*: true} -> false
 *   {a: one, b: two} match {a: ~n} -> true
 *   {a: one, b: two} match {*: "[Ww][Oo]$"} -> true
 * </pre>
 * 注意：值可以是任意的谓词，但是键必须是精准的字符串或者完全不指定。允许重复键。
 *
 * @see net.minecraft.nbt.NbtHelper#matches(NbtElement, NbtElement, boolean)
 */
public record MatchCompoundNbtPredicate(ListMultimap<@Nullable String, @NotNull NbtPredicate> entries, boolean negated) implements NbtPredicate {
  @Override
  public @NotNull String asString() {
    return asString(false);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (negated ? "!" : "") + (requirePrefix ? ": " : "") + "{" + entries.entries().stream().map(pair -> {
      final String key = pair.getKey();
      final String keyAsString;
      final NbtPredicate value = pair.getValue();
      if (key == null) {
        keyAsString = "*";
      } else {
        if (StringUtil.isAllowedInUnquotedString(key)) {
          keyAsString = key;
        } else {
          keyAsString = NbtString.escape(key);
        }
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
    for (Map.Entry<String, NbtPredicate> entry : entries.entries()) {
      final String key = entry.getKey();
      final NbtPredicate valuePredicate = entry.getValue();
      if (key != null) {
        final NbtElement actualElement = nbtCompound.get(key);
        if (actualElement == null || !valuePredicate.test(actualElement)) {
          return negated;
        }
      } else {
        boolean valueFound = false;
        for (String keyInNbtCompound : nbtCompound.getKeys()) {
          final NbtElement element = nbtCompound.get(keyInNbtCompound);
          if (element != null && valuePredicate.test(element)) {
            valueFound = true;
          }
        }
        if (!valueFound) {
          return negated;
        }
      }
    }
    return !negated;
  }
}
