package pers.solid.ecmd.predicate.nbt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 匹配复合标签谓词类似于原版的复合标签匹配。当实际值包含了所有的预期值时，返回 true。当实际值不为 NbtCompound 时，返回 false。其中，预期值中的键可以不指定，如果不指定，则表示无论是什么键，只要有这样一个值，就是 true。
 * <pre>
 *   {a: one, b: two} match {a: one} -> true
 *   {a: one, b: two} match {a: three} -> false
 *   {a: one, b: two} match {a: one, b: one} -> false
 *   {a: one, b: two} match {a: *} -> true
 *   {a: one, b: two} match {c: *} -> false
 *   {a: one, b: two} match {*: one} -> true
 *   {a: one, b: two} match {*: true} -> false
 *   {a: one, b: two} match {a: ~n} -> true
 *   {a: one, b: two} match {*: "[Ww][Oo]$"} -> true
 * </pre>
 * 注意：值可以是任意的谓词，但是键必须是精准的字符串或者完全不指定。允许重复键。
 *
 * @see net.minecraft.nbt.NbtHelper#matches(NbtElement, NbtElement, boolean)
 */
public record MatchCompoundNbtPredicate(ListMultimap<@Nullable String, @NotNull NbtPredicate> entries, boolean inverted) implements NbtPredicate {
  public static final MapCodec<MatchCompoundNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.unboundedMap(Codec.STRING, NbtPredicate.CODEC.listOf()).<ListMultimap<String, NbtPredicate>>xmap(map -> map.entrySet().stream().collect(ImmutableListMultimap.flatteningToImmutableListMultimap(Map.Entry::getKey, entry -> entry.getValue().stream())), map -> Maps.transformValues(map.asMap(), ImmutableList::copyOf)).fieldOf("entries").forGetter(matchCompoundNbtPredicate -> matchCompoundNbtPredicate.entries),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(MatchCompoundNbtPredicate::inverted)
  ).apply(i, MatchCompoundNbtPredicate::new));

  @Override
  public @NotNull String asString() {
    return asString(false);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (inverted ? "!" : "") + (requirePrefix ? ": " : "") + "{" + entries.entries().stream().map(pair -> {
      final String key = pair.getKey();
      final String keyAsString;
      final NbtPredicate value = pair.getValue();
      if (key == null) {
        keyAsString = "*";
      } else {
        keyAsString = ParsingUtil.quoteStringIfNeeded(key);
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
      return inverted;
    for (Map.Entry<String, NbtPredicate> entry : entries.entries()) {
      final String key = entry.getKey();
      final NbtPredicate valuePredicate = entry.getValue();
      if (key != null) {
        final NbtElement actualElement = nbtCompound.get(key);
        if (actualElement == null || !valuePredicate.test(actualElement)) {
          return inverted;
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
          return inverted;
        }
      }
    }
    return !inverted;
  }

  @Override
  public @NotNull NbtPredicateType<MatchCompoundNbtPredicate> getType() {
    return Type.MATCH_COMPOUND_TYPE;
  }

  public enum Type implements NbtPredicateType<MatchCompoundNbtPredicate> {
    MATCH_COMPOUND_TYPE;

    @Override
    public MapCodec<MatchCompoundNbtPredicate> getCodec() {
      return CODEC;
    }
  }
}
