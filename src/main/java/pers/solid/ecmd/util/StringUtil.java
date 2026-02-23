package pers.solid.ecmd.util;

import net.minecraft.Util;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.WrappedMinMaxBounds;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * 与字符串有关的实用类。
 */
public final class StringUtil {
  /**
   * 在本模组中将数字格式化的数字格式，需要避免使用分组并避免使用科学记数法，以与命令中的内容保持一致。
   */
  public static final NumberFormat nf = Util.make(NumberFormat.getInstance(Locale.ROOT), numberFormat -> {
    numberFormat.setMaximumFractionDigits(Integer.MAX_VALUE);
    numberFormat.setGroupingUsed(false);
  });

  private StringUtil() {
  }

  public static String wrapVector(Vec3i vec3i) {
    return vec3i.getX() + " " + vec3i.getY() + " " + vec3i.getZ();
  }

  public static String wrapVector(Position position) {
    return nf.format(position.x()) + " " + nf.format(position.y()) + " " + nf.format(position.z());
  }

  public static <T extends Number> String wrapRange(MinMaxBounds<T> numberRange) {
    final Optional<T> min = numberRange.min();
    final Optional<T> max = numberRange.max();
    if (min.isPresent() && min.equals(max)) {
      return min.get().toString();
    }
    return min.map(StringUtil.nf::format).orElse("") + ".." + max.map(StringUtil.nf::format).orElse("");
  }

  public static String wrapRange(WrappedMinMaxBounds numberRange) {
    final Float min = numberRange.min();
    final Float max = numberRange.max();
    if (min != null && min.equals(max)) {
      return min.toString();
    }
    return Objects.toString(min, "") + ".." + Objects.toString(max, "");
  }
}
