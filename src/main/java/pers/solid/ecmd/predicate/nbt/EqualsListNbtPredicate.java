package pers.solid.ecmd.predicate.nbt;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

public record EqualsListNbtPredicate(@NotNull List<@NotNull NbtPredicate> expected, boolean negated) implements NbtPredicate {
  @Override
  public @NotNull String asString() {
    return asString(true);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (negated ? "!" : "") + (requirePrefix ? "= " : "") + "[" + expected.stream().map(NbtPredicate::asString).collect(Collectors.joining(", ")) + "]";
  }

  @Override
  public boolean test(@NotNull NbtElement nbtElement) {
    if (!(nbtElement instanceof final NbtList nbtList))
      return negated;
    if (nbtList.size() != expected.size())
      return negated;
    final ListIterator<@NotNull NbtPredicate> listIterator = expected.listIterator();
    while (listIterator.hasNext()) {
      final int nextIndex = listIterator.nextIndex();
      if (!listIterator.next().test(nbtList.get(nextIndex))) {
        return negated;
      }
    }
    return !negated;
  }
}
