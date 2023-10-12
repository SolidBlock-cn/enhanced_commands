package pers.solid.ecmd.predicate.property;

import com.google.common.base.Preconditions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.NbtConvertible;

public interface PropertyPredicate<T extends Comparable<T>> extends ExpressionConvertible, NbtConvertible {
  boolean test(BlockState blockState);

  Property<T> property();

  static @NotNull PropertyPredicate<?> fromNbt(@NotNull NbtCompound nbtCompound, @NotNull Block block) {
    Preconditions.checkArgument(nbtCompound.contains("property", NbtElement.STRING_TYPE), "In the nbt, string value named 'property' is required!");
    final String propertyName = nbtCompound.getString("property");
    final Property<?> property = block.getStateManager().getProperty(propertyName);
    Preconditions.checkNotNull(property, "Unknown property '%s' for block %s.", propertyName, block);
    if (nbtCompound.contains("exists")) {
      return new ExistencePropertyPredicate<>(property, nbtCompound.getBoolean("exists"));
    } else {
      final Comparator comparator = Comparator.NAME_TO_VALUE.getOrDefault(nbtCompound.getString("comparator"), Comparator.EQ);
      return getValuePropertyPredicate(property, comparator, nbtCompound.getString("value"));
    }
  }

  private static <T extends Comparable<T>> ValuePropertyPredicate<T> getValuePropertyPredicate(Property<T> property, Comparator comparator, String name) {
    return new ValuePropertyPredicate<>(property, comparator, property.parse(name).orElseThrow(() -> new IllegalArgumentException("Unknown value '%s' for property '%s'.".formatted(name, property.getName()))));
  }
}
