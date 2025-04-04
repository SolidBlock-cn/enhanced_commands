package pers.solid.ecmd.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public sealed interface WeightedList<E> {
  E getRandom(Random random);

  E getElementAt(double position);

  Stream<String> asStringStream(Function<E, String> converter);

  default String asString(Function<E, String> converter) {
    return asStringStream(converter).collect(Collectors.joining(", "));
  }

  static <E> MapCodec<WeightedList<E>> createMapCodec(Codec<E> elementCodec) {
    final MapCodec<Uniform<E>> uniform = Uniform.createMapCodec(elementCodec);
    final MapCodec<Weighted<E>> weighted = Weighted.createMapCodec(elementCodec);
    return Codec.BOOL.dispatchMap("weighted", x -> x instanceof WeightedList.Weighted<?>, b -> b ? weighted : uniform);
  }

  record Uniform<E>(List<E> elements) implements WeightedList<E> {
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

    public static <E> MapCodec<Uniform<E>> createMapCodec(Codec<E> elemenetCodec) {
      return RecordCodecBuilder.mapCodec(i -> i.group(elemenetCodec.listOf().fieldOf("elements").forGetter(Uniform::elements)).apply(i, Uniform::new));
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

      // 注意：pairs 中的各浮点数的总和应该为 1。
      for (ObjectDoublePair<E> pair : entries) {
        stackedHeight += pair.rightDouble();
        if (position < stackedHeight) {
          return pair.left();
        }
      }

      return null;
    }

    @Override
    public Stream<String> asStringStream(Function<E, String> converter) {
      return entries.stream().map(pair -> pair.left() + " " + pair.rightDouble());
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
