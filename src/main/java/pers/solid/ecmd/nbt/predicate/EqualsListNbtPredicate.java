package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

public record EqualsListNbtPredicate(List<NbtPredicate> values) implements NbtPredicate {
  public static final MapCodec<EqualsListNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtPredicate.CODEC.listOf().fieldOf("values").forGetter(EqualsListNbtPredicate::values)
  ).apply(i, EqualsListNbtPredicate::new));

  @Override
  public String expressAsString() {
    return asString(true);
  }

  @Override
  public String asString(boolean requirePrefix) {
    return (requirePrefix ? "= " : "") + "[" + values.stream().map(nbtPredicate -> nbtPredicate.asString(true)).collect(Collectors.joining(", ")) + "]";
  }

  @Override
  public boolean test(Tag nbtElement) {
    if (!(nbtElement instanceof final ListTag nbtList))
      return false;
    if (nbtList.size() != values.size())
      return false;
    final ListIterator<NbtPredicate> listIterator = values.listIterator();
    while (listIterator.hasNext()) {
      final int nextIndex = listIterator.nextIndex();
      if (!listIterator.next().test(nbtList.get(nextIndex))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public NbtPredicateType<EqualsListNbtPredicate> getType() {
    return NbtPredicateTypes.EQUALS_LIST;
  }
}
