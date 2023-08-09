package pers.solid.ecmd.predicate.property;

import com.google.common.base.Preconditions;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.SerializablePredicate;
import pers.solid.ecmd.util.NbtConvertible;

public interface PropertyNamePredicate extends SerializablePredicate, NbtConvertible {
  boolean test(BlockState blockState);

  TestResult testAndDescribe(BlockState blockState, BlockPos blockPos);

  String propertyName();

  static @NotNull PropertyNamePredicate fromNbt(@NotNull NbtCompound nbtCompound) {
    Preconditions.checkArgument(nbtCompound.contains("property", NbtElement.STRING_TYPE), "In the nbt, string value named 'property' is required!");
    final String propertyName = nbtCompound.getString("property");
    if (nbtCompound.contains("exists")) {
      return new NameExistencePropertyPredicate(propertyName, nbtCompound.getBoolean("exists"));
    } else {
      final Comparator comparator = Comparator.NAME_TO_VALUE.getOrDefault(nbtCompound.getString("comparator"), Comparator.EQ);
      return new ValueNamePropertyPredicate(propertyName, comparator, nbtCompound.getString("value"));
    }
  }
}
