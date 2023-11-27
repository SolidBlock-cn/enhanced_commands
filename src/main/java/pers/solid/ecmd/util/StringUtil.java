package pers.solid.ecmd.util;

import net.minecraft.command.FloatRangeArgument;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.math.Position;

import java.util.Objects;

/**
 * 与字符串有关的实用类。
 */
public final class StringUtil {
  private StringUtil() {
  }

  public static String wrapPosition(Position position) {
    return position.getX() + " " + position.getY() + " " + position.getZ();
  }

  public static <T extends Number> String wrapRange(NumberRange<T> numberRange) {
    final var min = numberRange.getMin();
    final var max = numberRange.getMax();
    if (min != null && min.equals(max)) {
      return min.toString();
    }
    return Objects.toString(min, "") + ".." + Objects.toString(max, "");
  }

  public static String wrapRange(FloatRangeArgument numberRange) {
    final var min = numberRange.getMin();
    final var max = numberRange.getMax();
    if (min != null && min.equals(max)) {
      return min.toString();
    }
    return Objects.toString(min, "") + ".." + Objects.toString(max, "");
  }
}
