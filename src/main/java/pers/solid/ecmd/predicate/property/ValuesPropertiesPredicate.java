package pers.solid.ecmd.predicate.property;

import com.google.common.collect.Collections2;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

public record ValuesPropertiesPredicate<T extends Comparable<T>>(Property<T> property, Collection<T> values, boolean inverted) implements PropertyPredicate<T> {
  @Override
  public boolean test(BlockState blockState) {
    return blockState.contains(property) && values.contains(blockState.get(property)) != inverted;
  }

  @Override
  public @NotNull String asString() {
    return property.getName() + (inverted ? "!=" : "=") + values.stream().map(property::name).collect(Collectors.joining("|"));
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", property.getName());
    nbtCompound.putString("comparator", inverted ? Comparator.NE.asString() : Comparator.EQ.asString());
    final NbtList nbtList = new NbtList();
    nbtList.addAll(Collections2.transform(values, input -> NbtString.of(property.name(input))));
    nbtCompound.put("value", nbtList);
  }
}
