package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import pers.solid.ecmd.nbt.function.PositionalListEntry;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 匹配一个列表，与原版的行为类似。只要预期元素在实际的列表中都是存在的，那么就为 true。如果实际值不是列表，那么就是 false。
 * <pre>
 *   "string" match [1, 2] -> false
 *   [1, 2] match [1, 2] -> true
 *   [2, 1] match [1, 2, 3] -> true
 *   [2, 1, 1, 2] match [1, 2, 3] -> true
 *   [2b] match [1, 2] -> false
 *   [=2b] match [1, 2] -> true
 *   [>3, <5] match [5, 9] -> true
 *   [] match [3, 4, 5] -> true
 *   [[2, 3], [4, 5]] match [[4, 6, 5], [4, 3, 2]] -> true
 *   [[2, 3], [4, 5]] match [[2, 3, 4, 5]] -> true
 *   [=[2, 3], =[4, 5]] match [[2, 3, 4, 5]] -> false
 * </pre>
 */
public record MatchListNbtPredicate(List<NbtPredicate> expected, List<PositionalListEntry<NbtPredicate>> positionalExpected, boolean inverted) implements NbtPredicate {
  public static final MapCodec<MatchListNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtPredicate.CODEC.listOf().fieldOf("expected").forGetter(MatchListNbtPredicate::expected),
      PositionalListEntry.codec(NbtPredicate.CODEC).listOf().fieldOf("positional_expected").forGetter(MatchListNbtPredicate::positionalExpected),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(MatchListNbtPredicate::inverted)
  ).apply(i, MatchListNbtPredicate::new));

  @Override
  public String asString() {
    return asString(false);
  }

  @Override
  public String asString(boolean requirePrefix) {
    return (inverted ? "!" : "") + (requirePrefix ? ": " : "") + "[" + Stream.concat(expected.stream().map(NbtPredicate::asString), positionalExpected.stream().map(pair -> {
      final String valueAsString = pair.value().asString(true);
      return pair.index() + (valueAsString.startsWith(":") ? "" : " ") + valueAsString;
    })).collect(Collectors.joining(", ")) + "]";
  }

  @Override
  public boolean test(Tag nbtElement) {
    if (!(nbtElement instanceof ListTag nbtList)) {
      return inverted;
    }
    for (NbtPredicate nbtPredicate : expected) {
      boolean elementMatched = false;
      for (Tag actualElement : nbtList) {
        if (nbtPredicate.test(actualElement)) {
          elementMatched = true;
          break;
        }
      }
      if (!elementMatched)
        return inverted;
    }
    final int size = nbtList.size();
    for (PositionalListEntry<NbtPredicate> pair : positionalExpected) {
      int expectedIndex = pair.index();
      if (expectedIndex < 0) {
        expectedIndex += nbtList.size();
      }
      if (expectedIndex >= 0 && size > expectedIndex) {
        if (!pair.value().test(nbtList.get(expectedIndex))) {
          return inverted;
        }
      } else {
        return inverted;
      }
    }
    return !inverted;
  }

  @Override
  public NbtPredicateType<MatchListNbtPredicate> getType() {
    return MatchListNbtPredicate.Type.MATCH_LIST_TYPE;
  }

  public enum Type implements NbtPredicateType<MatchListNbtPredicate> {
    MATCH_LIST_TYPE;

    @Override
    public MapCodec<MatchListNbtPredicate> getCodec() {
      return CODEC;
    }
  }
}
