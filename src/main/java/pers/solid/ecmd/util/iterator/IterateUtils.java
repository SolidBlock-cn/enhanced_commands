package pers.solid.ecmd.util.iterator;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.function.FailableFunction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Stream;

public final class IterateUtils {
  private IterateUtils() {
  }

  public static <T> Stream<T> singletonNullStream() {
    return Stream.of((T) null);
  }

  public static <T> Stream<T> singletonPeekingStream(Runnable runnable) {
    return IterateUtils.<T>singletonNullStream().peek(o -> runnable.run());
  }

  public static <T> Iterator<T> singletonPeekingIterator(Runnable runnable) {
    return IterateUtils.<T>singletonPeekingStream(runnable).iterator();
  }

  /**
   * Exhaust the iterator, causing {@code hasNext} and {@code next} called until {@code hasNext} returns {@code false}. If it is an iterator that always "has next" (such as an infinity cycling iterator), it will cause an infinite loop.
   */
  public static void exhaust(Iterator<?> iterator) {
    while (iterator.hasNext()) iterator.next();
  }

  /**
   * Exhaust the stream, causing all {@code peek}, {@code filter} and {@code map} (if any) to be evaluated.
   */
  public static void exhaust(Stream<?> stream) {
    stream.forEach(o -> {});
  }

  public static <E> Iterable<E> batchAndSkip(Iterable<E> forward, int batchSize, int skipTimes) {
    return () -> new SkippingIterator<>(new BatchedIterator<>(forward.iterator(), batchSize), skipTimes);
  }

  public static <E> Iterator<E> batchAndSkip(Iterator<E> forward, int batchSize, int skipTimes) {
    return new SkippingIterator<>(new BatchedIterator<>(forward, batchSize), skipTimes);
  }

  public static <T, R, E extends Throwable> ArrayList<R> transformFailableArrayList(Iterable<T> iterable, FailableFunction<T, R, E> failableFunction) throws E {
    final ArrayList<R> list = new ArrayList<>();
    for (T t : iterable) {
      list.add(failableFunction.apply(t));
    }
    return list;
  }

  public static <T, R, E extends Throwable> ImmutableList<R> transformFailableImmutableList(Iterable<T> iterable, FailableFunction<T, R, E> failableFunction) throws E {
    final ImmutableList.Builder<R> builder = new ImmutableList.Builder<>();
    for (T t : iterable) {
      builder.add(failableFunction.apply(t));
    }
    return builder.build();
  }
}
