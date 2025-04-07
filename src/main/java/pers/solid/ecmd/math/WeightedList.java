package pers.solid.ecmd.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.Range;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public sealed interface WeightedList<E> {
  E getRandom(Random random);

  /**
   * 获取位于绝对位置的元素。当该列表为均匀列表时，相同于获取特定元素下标的元素。
   *
   * @param position 元素位置，对于均匀列表相当于元素下标。
   * @return 指定位置的元素。
   */
  E getElementAt(double position);

  double size();

  /**
   * 获取位于相对于整个列表位置（0 到 1 之间的双精度浮点）的元素。
   *
   * @param position 在整个列表中的相对位置，一般是 0 到 1 之间。
   * @return 指定位置的元素。
   */
  default E getClampedElement(@Range(from = 0, to = 1) double position) {
    return getElementAt(position * size());
  }

  Stream<String> asStringStream(Function<E, String> converter);

  default String asString(Function<E, String> converter) {
    return asStringStream(converter).collect(Collectors.joining(", "));
  }

  /**
   * 转换该列表中的元素，不影响次序、权重等信息。返回的结果是不可修改的。
   */
  <R, Ex extends Throwable> WeightedList<R> transform(FailableFunction<E, R, Ex> transformer) throws Ex;

  static <E> MapCodec<WeightedList<E>> createMapCodec(Codec<E> elementCodec) {
    final MapCodec<Uniform<E>> uniform = Uniform.createMapCodec(elementCodec);
    final MapCodec<Weighted<E>> weighted = Weighted.createMapCodec(elementCodec);
    return Codec.BOOL.dispatchMap("weighted", x -> x instanceof WeightedList.Weighted<?>, b -> b ? weighted : uniform);
  }

  record Uniform<E>(List<E> elements) implements WeightedList<E> {
    @SafeVarargs
    public Uniform(E... elements) {
      this(List.of(elements));
    }

    @Override
    public E getRandom(Random random) {
      return elements.get(random.nextInt(elements.size()));
    }

    @Override
    public E getElementAt(double position) {
      if (elements.isEmpty()) {
        return null;
      }
      return elements.get(MathHelper.floor(MathHelper.floorMod(position, elements.size())));
    }

    @Override
    public Stream<String> asStringStream(Function<E, String> converter) {
      return elements.stream().map(converter);
    }

    @Override
    public <R, Ex extends Throwable> WeightedList<R> transform(FailableFunction<E, R, Ex> transformer) throws Ex {
      return new Uniform<>(IterateUtils.transformFailableImmutableList(elements, transformer));
    }

    public static <E> MapCodec<Uniform<E>> createMapCodec(Codec<E> elemenetCodec) {
      return RecordCodecBuilder.mapCodec(i -> i.group(elemenetCodec.listOf().fieldOf("elements").forGetter(Uniform::elements)).apply(i, Uniform::new));
    }

    @Override
    public double size() {
      return elements.size();
    }
  }

  final class Weighted<E> implements WeightedList<E> {
    private final List<ObjectDoublePair<E>> entries;
    private transient final double sum;

    public Weighted(List<ObjectDoublePair<E>> entries) {
      this.entries = entries;
      this.sum = entries.stream().mapToDouble(ObjectDoublePair::rightDouble).sum();
    }

    @SafeVarargs
    public Weighted(ObjectDoublePair<E>... entries) {
      this(List.of(entries));
    }

    @Override
    public E getRandom(Random random) {
      final double height = random.nextDouble() * sum;
      return getElementAt(height);
    }

    @Override
    public E getElementAt(double position) {
      position = MathHelper.floorMod(position, sum);
      double stackedHeight = 0;

      for (ObjectDoublePair<E> pair : entries) {
        stackedHeight += pair.rightDouble();
        if (position < stackedHeight) {
          return pair.left();
        }
      }

      return null;
    }

    @Override
    public double size() {
      return sum;
    }

    @Override
    public Stream<String> asStringStream(Function<E, String> converter) {
      return entries.stream().map(pair -> pair.left() + " " + pair.rightDouble());
    }

    @Override
    public <R, Ex extends Throwable> WeightedList<R> transform(FailableFunction<E, R, Ex> transformer) throws Ex {
      return new Weighted<>(IterateUtils.transformFailableImmutableList(entries, pair -> ObjectDoublePair.of(transformer.apply(pair.left()), pair.rightDouble())));
    }

    public List<ObjectDoublePair<E>> entries() {
      return entries;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null || obj.getClass() != this.getClass()) return false;
      var that = (Weighted<?>) obj;
      return Objects.equals(this.entries, that.entries);
    }

    @Override
    public int hashCode() {
      return Objects.hash(entries);
    }

    @Override
    public String toString() {
      return "Weighted[" + "entries=" + entries + ']';
    }

    public static <E> MapCodec<Weighted<E>> createMapCodec(Codec<E> elementCodec) {
      Codec<ObjectDoublePair<E>> pairCodec = RecordCodecBuilder.create(j -> j.apply2(ObjectDoublePair::of, elementCodec.fieldOf("element").forGetter(ObjectDoublePair::left), Codec.DOUBLE.optionalFieldOf("weight", 1d).forGetter(ObjectDoublePair::rightDouble)));
      return RecordCodecBuilder.mapCodec(i -> i.group(pairCodec.listOf().fieldOf("entries").forGetter(Weighted::entries)).apply(i, Weighted::new));
    }
  }
}
