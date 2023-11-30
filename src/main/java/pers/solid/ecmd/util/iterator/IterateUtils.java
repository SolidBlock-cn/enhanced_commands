package pers.solid.ecmd.util.iterator;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * 此类包含一些与迭代器、可迭代对象、集合、流有关的静态实用方法。
 */
public final class IterateUtils {
  private IterateUtils() {
  }


  @Contract(pure = true)
  public static <T> Stream<T> singletonNullStream() {
    return Stream.of((T) null);
  }

  /**
   * 返回一个仅产生一个 {@code null} 的流，这个流在运行过程中会执行一次 {@code runnable}。例如：
   * <pre>{@code
   *  singletonPeekingStream(() -> System.out.println("Hello world!")).toList();
   *  // 输出 "Hello world!"，同时返回一个含有单个 null 的列表。
   * }</pre>
   *
   * @param runnable 流在运行中需要运行的 {@link Runnable}。
   * @return 仅产生一个 {@code null} 的流。
   */
  @Contract(pure = true)
  public static <T> Stream<T> singletonPeekingStream(Runnable runnable) {
    return IterateUtils.<T>singletonNullStream().peek(o -> runnable.run());
  }

  /**
   * 返回一个仅产生一个 {@code null} 的迭代器，这个迭代器在运行过程中会执行一次 {@code runnable}。例如：
   * <pre>{@code
   *  var iterator = singletonPeekingIterator(() -> System.out.println("Hello world!"));
   *  while (iterator.hasNext()) {
   *    System.out.println(iterator.next());
   *  }
   *  // 输出 "Hello world!"，同时输入一个 null。
   * }</pre>
   *
   * @param runnable 迭代器在迭代过程中需要运行的 {@link Runnable}。
   * @return 仅产生一个 {@code null} 的流。
   */
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

  /**
   * 调整 {@link Iterable} 的运行过程。
   *
   * @param forward   返回的对象所基于的可迭代对象。
   * @param batchSize 一次运行的次数。
   * @param skipTimes 一次运行后所跳过的次数。
   */
  @Contract(pure = true)
  public static <E> Iterable<E> batchAndSkip(Iterable<E> forward, int batchSize, int skipTimes) {
    return () -> new SkippingIterator<>(new BatchedIterator<>(forward.iterator(), batchSize), skipTimes);
  }

  /**
   * 调用 {@link Iterator} 的运行过程。此过程会基于一个 {@code forward}，返回的 {@code iterator} 中，每调用一次 {@link Iterator#next()}，会调用多次 {@code forward.next()}，之后的几次 {@code next()} 都将被忽略并返回 {@code null}。
   *
   * @param forward   返回的对象所基于的迭代器。
   * @param batchSize 迭代器运行一次 {@code next()} 时，{@code forward} 所运行 {@code next()} 的次数。
   * @param skipTimes 迭代器运行一次 {@code next()} 后，之后多少次的 {@code next()} 将不执行任何操作（不调用 {@code forward.next()}）并返回 {@code null}。
   */
  @Contract(pure = true)
  public static <E> Iterator<E> batchAndSkip(Iterator<E> forward, int batchSize, int skipTimes) {
    return new SkippingIterator<>(new BatchedIterator<>(forward, batchSize), skipTimes);
  }

  /**
   * 将一个可迭代对象中的各对象通过指定的函数转换进行转换并收集到新的列表中，返回的列表是可修改的。中途如果遇到异常，则会直接将其抛出并中止迭代过程。此过程类似于 {@link com.google.common.collect.Iterables#transform(Iterable, Function)} 或 {@link Stream#map(java.util.function.Function)}，但是允许中途抛出异常。
   *
   * @param iterable         包含函数应用的对象的可迭代对象。
   * @param failableFunction 用于转换的函数，
   * @param <T>              转换前的对象的类型。
   * @param <R>              转换后的对象的类型。
   * @param <E>              转换过程中可能抛出的异常。
   * @return 包含转换后的对象的 {@link ArrayList}，可修改。
   */
  @Contract(pure = true)
  public static <T, R, E extends Throwable> ArrayList<R> transformFailableArrayList(Iterable<T> iterable, FailableFunction<T, R, E> failableFunction) throws E {
    final ArrayList<R> list = new ArrayList<>();
    for (T t : iterable) {
      list.add(failableFunction.apply(t));
    }
    return list;
  }

  /**
   * 将一个可迭代对象中的各对象通过指定的函数转换进行转换并收集到新的列表中，返回的列表是不可修改的。中途如果遇到异常，则会直接将其抛出并中止迭代过程。此过程类似于 {@link com.google.common.collect.Iterables#transform(Iterable, Function)} 或 {@link Stream#map(java.util.function.Function)}，但是允许中途抛出异常。
   *
   * @param iterable         包含函数应用的对象的可迭代对象。
   * @param failableFunction 用于转换的函数，
   * @param <T>              转换前的对象的类型。
   * @param <R>              转换后的对象的类型。
   * @param <E>              转换过程中可能抛出的异常。
   * @return 包含转换后的对象的 {@link ImmutableList}，不可修改。
   */
  @Contract(pure = true)
  public static <T, R, E extends Throwable> ImmutableList<R> transformFailableImmutableList(Iterable<T> iterable, FailableFunction<T, R, E> failableFunction) throws E {
    final ImmutableList.Builder<R> builder = new ImmutableList.Builder<>();
    for (T t : iterable) {
      builder.add(failableFunction.apply(t));
    }
    return builder.build();
  }
}
