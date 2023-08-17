package pers.solid.ecmd.predicate.nbt;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.NotNull;

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
public record MatchListNbtPredicate(List<@NotNull NbtPredicate> expected, List<IntObjectPair<@NotNull NbtPredicate>> positionalExpected, boolean negated) implements NbtPredicate {
  @Override
  public @NotNull String asString() {
    return asString(false);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (negated ? "!" : "") + (requirePrefix ? ": " : "") + "[" + Stream.concat(expected.stream().map(NbtPredicate::asString), positionalExpected.stream().map(pair -> pair.leftInt() + " " + pair.right().asString(true))).collect(Collectors.joining(", ")) + "]";
  }

  @Override
  public boolean test(@NotNull NbtElement nbtElement) {
    if (!(nbtElement instanceof NbtList nbtList)) {
      return negated;
    }
    for (NbtPredicate nbtPredicate : expected) {
      boolean elementMatched = false;
      for (NbtElement actualElement : nbtList) {
        if (nbtPredicate.test(actualElement)) {
          elementMatched = true;
          break;
        }
      }
      if (!elementMatched)
        return negated;
    }
    final int size = nbtList.size();
    for (IntObjectPair<NbtPredicate> pair : positionalExpected) {
      final int expectedIndex = pair.leftInt();
      if (size > expectedIndex) {
        if (!pair.right().test(nbtList.get(expectedIndex))) {
          return negated;
        }
      } else {
        return negated;
      }
    }
    return !negated;
  }
}
