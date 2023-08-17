package pers.solid.ecmd.predicate.nbt;

import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.visitor.StringNbtWriter;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.predicate.property.Comparator;

/**
 * 匹配一个 NBT 数值是否在数值上与指定的值相等，而不考虑其类型。例如：
 * <pre>
 *   1 match 1s -> true
 *   3b match 3.0f -> true
 *   8s match 8L -> true
 * </pre>
 */
public record ComparisonNbtPredicate(Comparator comparator, NbtElement expected) implements NbtPredicate {
  @Override
  public @NotNull String asString() {
    return comparator.asString() + " " + new StringNbtWriter().apply(expected);
  }

  @Override
  public boolean test(@NotNull NbtElement nbtElement) {
    if (nbtElement instanceof AbstractNbtNumber actualNumber && expected instanceof AbstractNbtNumber expectedNumber) {
      final byte actualType = actualNumber.getType();
      final byte expectedType = expectedNumber.getType();
      if (actualType == NbtElement.DOUBLE_TYPE || expectedType == NbtElement.DOUBLE_TYPE) {
        return comparator.compareDouble(actualNumber.doubleValue(), expectedNumber.doubleValue());
      } else if (actualType == NbtElement.FLOAT_TYPE || expectedType == NbtElement.FLOAT_TYPE) {
        return comparator.compareFloat(actualNumber.floatValue(), expectedNumber.floatValue());
      } else if (actualType == NbtElement.LONG_TYPE || expectedType == NbtElement.LONG_TYPE) {
        return comparator.compareLong(actualNumber.longValue(), expectedNumber.longValue());
      } else if (actualType == NbtElement.INT_TYPE || expectedType == NbtElement.INT_TYPE) {
        return comparator.compareInt(actualNumber.intValue(), expectedNumber.intValue());
      } else if (actualType == NbtElement.SHORT_TYPE || expectedType == NbtElement.SHORT_TYPE) {
        return comparator.compareShort(actualNumber.shortValue(), expectedNumber.shortValue());
      } else if (actualType == NbtElement.BYTE_TYPE || expectedType == NbtElement.BYTE_TYPE) {
        return comparator.compareByte(actualNumber.byteValue(), expectedNumber.byteValue());
      } else {
        return false;
      }
    } else if (nbtElement instanceof NbtString actualString && expected instanceof NbtString expectedString) {
      return comparator.test(actualString.asString(), expectedString.asString());
    } else {
      return false;
    }
  }
}
