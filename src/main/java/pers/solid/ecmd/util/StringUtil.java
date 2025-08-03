package pers.solid.ecmd.util;

import net.minecraft.command.FloatRangeArgument;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3i;

import java.util.Objects;
import java.util.Optional;

/**
 * 与字符串有关的实用类。
 */
public final class StringUtil {
  private StringUtil() {
  }

  public static String wrapVector(Vec3i vec3i) {
    return vec3i.getX() + " " + vec3i.getY() + " " + vec3i.getZ();
  }

  public static String wrapVector(Position position) {
    return position.getX() + " " + position.getY() + " " + position.getZ();
  }

  public static <T extends Number> String wrapRange(NumberRange<T> numberRange) {
    final Optional<T> min = numberRange.min();
    final Optional<T> max = numberRange.max();
    if (min.isPresent() && min.equals(max)) {
      return min.get().toString();
    }
    return min.map(Objects::toString).orElse("") + ".." + max.map(Objects::toString).orElse("");
  }

  public static String wrapRange(FloatRangeArgument numberRange) {
    final Float min = numberRange.min();
    final Float max = numberRange.max();
    if (min != null && min.equals(max)) {
      return min.toString();
    }
    return Objects.toString(min, "") + ".." + Objects.toString(max, "");
  }
}
