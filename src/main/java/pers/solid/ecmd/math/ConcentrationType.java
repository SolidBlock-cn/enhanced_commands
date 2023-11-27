package pers.solid.ecmd.math;

import it.unimi.dsi.fastutil.doubles.DoubleIterable;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatIterable;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.command.CommandException;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

import java.util.Iterator;

/**
 * 数据的聚合类型，用于处理一组原始类型的数据。注意：必须是直接处理原始类型的数据，不对任何值进行装箱。为了提高性能，这里尽可能地避免了将可迭代的对象转换为流。。
 */
public enum ConcentrationType implements StringIdentifiable {
  FIRST("first", false) {
    @Override
    public double concentrateLong(LongIterable values) {
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      return iterator.nextLong();
    }

    @Override
    public double concentrateInt(IntIterable values) {
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      return iterator.nextInt();
    }

    @Override
    public double concentrateFloat(FloatIterable values) {
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      return iterator.nextFloat();
    }

    @Override
    public double concentrateDouble(DoubleIterable values) {
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      return iterator.nextDouble();
    }
  },
  LAST("last", false) {
    @Override
    public double concentrateLong(LongIterable values) {
      if (values instanceof LongList longList && !longList.isEmpty()) return longList.getLong(longList.size() - 1);
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      long value = 0;
      while (iterator.hasNext()) value = iterator.nextLong();
      return value;
    }

    @Override
    public double concentrateInt(IntIterable values) {
      if (values instanceof IntList intList && !intList.isEmpty()) return intList.getInt(intList.size() - 1);
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      int value = 0;
      while (iterator.hasNext()) value = iterator.nextInt();
      return value;
    }

    @Override
    public double concentrateFloat(FloatIterable values) {
      if (values instanceof FloatList floatList && !floatList.isEmpty()) return floatList.getFloat(floatList.size() - 1);
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      float value = 0;
      while (iterator.hasNext()) value = iterator.nextFloat();
      return value;
    }

    @Override
    public double concentrateDouble(DoubleIterable values) {
      if (values instanceof DoubleList doubleList && !doubleList.isEmpty()) return doubleList.getDouble(doubleList.size() - 1);
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      double value = 0;
      while (iterator.hasNext()) value = iterator.nextDouble();
      return value;
    }
  },

  MIN("min", false) {
    @Override
    public double concentrateLong(LongIterable values) {
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      var r = iterator.nextLong();
      while (iterator.hasNext()) r = Math.min(r, iterator.nextLong());
      return r;
    }

    @Override
    public double concentrateInt(IntIterable values) {
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      var r = iterator.nextInt();
      while (iterator.hasNext()) r = Math.min(r, iterator.nextInt());
      return r;
    }

    @Override
    public double concentrateFloat(FloatIterable values) {
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      var r = iterator.nextFloat();
      while (iterator.hasNext()) r = Math.min(r, iterator.nextFloat());
      return r;
    }

    @Override
    public double concentrateDouble(DoubleIterable values) {
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      var r = iterator.nextDouble();
      while (iterator.hasNext()) r = Math.min(r, iterator.nextDouble());
      return r;
    }
  },
  MAX("max", false) {
    @Override
    public double concentrateLong(LongIterable values) {
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      var r = iterator.nextLong();
      while (iterator.hasNext()) r = Math.max(r, iterator.nextLong());
      return r;
    }

    @Override
    public double concentrateInt(IntIterable values) {
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      var r = iterator.nextInt();
      while (iterator.hasNext()) r = Math.max(r, iterator.nextInt());
      return r;
    }

    @Override
    public double concentrateFloat(FloatIterable values) {
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      var r = iterator.nextFloat();
      while (iterator.hasNext()) r = Math.max(r, iterator.nextFloat());
      return r;
    }

    @Override
    public double concentrateDouble(DoubleIterable values) {
      final var iterator = values.iterator();
      ensureHasNext(iterator);
      var r = iterator.nextDouble();
      while (iterator.hasNext()) r = Math.max(r, iterator.nextDouble());
      return r;
    }
  },

  AVERAGE("average", true) {
    @Override
    public double concentrateLong(LongIterable values) {
      int size = 0;
      long sum = 0;
      final var iterator = values.iterator();
      while (iterator.hasNext()) {
        size++;
        sum += iterator.nextLong();
      }
      return (double) sum / size;
    }

    @Override
    public double concentrateInt(IntIterable values) {
      int size = 0;
      int sum = 0;
      final var iterator = values.iterator();
      while (iterator.hasNext()) {
        size++;
        sum += iterator.nextInt();
      }
      return (double) sum / size;
    }

    @Override
    public double concentrateFloat(FloatIterable values) {
      int size = 0;
      float sum = 0;
      final var iterator = values.iterator();
      while (iterator.hasNext()) {
        size++;
        sum += iterator.nextFloat();
      }
      return (double) sum / size;
    }

    @Override
    public double concentrateDouble(DoubleIterable values) {
      int size = 0;
      double sum = 0;
      final var iterator = values.iterator();
      while (iterator.hasNext()) {
        size++;
        sum += iterator.nextDouble();
      }
      return sum / size;
    }
  },

  SUM("sum", false) {
    @Override
    public double concentrateLong(LongIterable values) {
      long sum = 0;
      var iterator = values.iterator();
      while (iterator.hasNext()) sum += iterator.nextLong();
      return sum;
    }

    @Override
    public double concentrateInt(IntIterable values) {
      int sum = 0;
      var iterator = values.iterator();
      while (iterator.hasNext()) sum += iterator.nextInt();
      return sum;
    }

    @Override
    public double concentrateFloat(FloatIterable values) {
      float sum = 0;
      var iterator = values.iterator();
      while (iterator.hasNext()) sum += iterator.nextFloat();
      return sum;
    }

    @Override
    public double concentrateDouble(DoubleIterable values) {
      double sum = 0;
      var iterator = values.iterator();
      while (iterator.hasNext()) sum += iterator.nextDouble();
      return sum;
    }
  };
  public static final Text NO_VALUE = Text.translatable("enhanced_commands.concentration_type.no_value");
  public static final Codec<ConcentrationType> CODEC = StringIdentifiable.createCodec(ConcentrationType::values);
  private final String name;
  private final Text displayName;
  /**
   * 如果此字段为 true，那么根据此聚合类型所计算出的结果一律为 {@code double}。
   */
  private final boolean forcesDouble;

  ConcentrationType(String name, boolean forcesDouble) {
    this.name = name;
    this.displayName = Text.translatable("enhanced_commands.concentration_type." + name);
    this.forcesDouble = forcesDouble;
  }

  public abstract double concentrateLong(LongIterable values);

  public abstract double concentrateInt(IntIterable values);

  public abstract double concentrateFloat(FloatIterable values);

  public abstract double concentrateDouble(DoubleIterable values);

  // 以下两个方法均用于以适当的方式将计算结果转化为字符串。尽管计算结果始终是 double，但在一些情况下，仍需要以原来的类型的方式呈现值。例如，几个整数的最大值尽管计算出来后是 double，但我们仍会尽可能以整数的方式呈现它，但考虑到最大值可能超过 Integer.MAX_VALUE，因此对于 int，仍使用 long。

  /**
   * 对于求平均值的情况，以 double 的方式呈现结果，在其他情况下则以 long 的方式呈现结果（此时不显示小数部分）。
   */
  public String longToString(double result) {
    return forcesDouble ? Double.toString(result) : Long.toString((long) result);
  }

  /**
   * 对于求平均值的情况，以 double 的方式呈现结果，在其他情况下则以 float 的方式呈现结果（此时显示的精度会低一些）。
   */
  public String floatToString(double result) {
    return forcesDouble ? Double.toString(result) : Float.toString((long) result);
  }

  private static void ensureHasNext(Iterator<?> iterator) {
    if (!iterator.hasNext()) throw new CommandException(NO_VALUE);
  }

  @Override
  public String asString() {
    return name;
  }

  public Text getDisplayName() {
    return displayName;
  }
}
