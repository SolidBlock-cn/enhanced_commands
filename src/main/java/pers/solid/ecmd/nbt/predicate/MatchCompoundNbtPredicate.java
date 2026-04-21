package pers.solid.ecmd.nbt.predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.ParsingUtil;

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
 * @see net.minecraft.nbt.NbtUtils#compareNbt(Tag, Tag, boolean)
 */
public record MatchCompoundNbtPredicate(ListMultimap<@Nullable String, NbtPredicate> entries) implements NbtPredicate {
  public static final MapCodec<MatchCompoundNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.unboundedMap(Codec.STRING, NbtPredicate.CODEC.listOf()).<ListMultimap<String, NbtPredicate>>xmap(map -> map.entrySet().stream().collect(ImmutableListMultimap.flatteningToImmutableListMultimap(Map.Entry::getKey, entry -> entry.getValue().stream())), map -> Maps.transformValues(map.asMap(), ImmutableList::copyOf)).fieldOf("entries").forGetter(matchCompoundNbtPredicate -> matchCompoundNbtPredicate.entries)
  ).apply(i, MatchCompoundNbtPredicate::new));

  @Override
  public String expressAsString() {
    return asString(false);
  }

  @Override
  public String asString(boolean requirePrefix) {
    return (requirePrefix ? ": " : "") + "{" + entries.entries().stream().map(pair -> {
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
  public boolean test(Tag nbtElement) {
    if (!(nbtElement instanceof final CompoundTag nbtCompound))
      return false;
    for (Map.Entry<@Nullable String, NbtPredicate> entry : entries.entries()) {
      final String key = entry.getKey();
      final NbtPredicate valuePredicate = entry.getValue();
      if (key != null) {
        final Tag actualElement = nbtCompound.get(key);
        if (actualElement == null || !valuePredicate.test(actualElement)) {
          return false;
        }
      } else {
        boolean valueFound = false;
        for (String keyInNbtCompound : nbtCompound.getAllKeys()) {
          final Tag element = nbtCompound.get(keyInNbtCompound);
          if (element != null && valuePredicate.test(element)) {
            valueFound = true;
          }
        }
        if (!valueFound) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public NbtPredicateType<MatchCompoundNbtPredicate> getType() {
    return NbtPredicateTypes.MATCH_COMPOUND;
  }
}
