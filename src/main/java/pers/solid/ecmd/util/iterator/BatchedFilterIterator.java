package pers.solid.ecmd.util.iterator;

import com.google.common.collect.AbstractIterator;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * <p>类似于 {@link BatchedIterator}，不过仅进行筛选操作。在一次迭代中，尝试多次找到符合 {@link #predicate} 的元素，如果有，则直接返回它，否则直接返回 {@code null}。
 * <p>与 {@code new BatchedIterator(Iterators.filter(...))} 的区域在于，它会一次性跳过所有不符合 {@code predicate} 的元素，找到符合的元素才进行计算。使用 {@code stream.filter} 也是类似的。
 * <p>在服务器中，如果一个 iterator 含有大量不符合 {@code predicate} 的元素，这会影响性能。因此，本类可以用来限制一次 {@code next} 中判断谓词的次数。
 * <p>例如，如果使用 {@code new BatchedIterator(Iterators.filter(...), 3)}，假设有 1 和 0 两种元素，只有 1 符合条件，一个横线表示迭代的一步：</p>
 * <blockquote><u>1 0 0 0 0 0 1 0 1</u> 0 <u>1 1 0 1</u> 0 0 0</blockquote>
 * <p>如果使用 {@code new BatchedFilterIterator(..., 3, ...)}</p>
 * <blockquote><u>1</u> <u>0 0 0</u> <u>0 1</u> <u>0 0 1</u> <u>0 1</u> <u>1</u> <u>0 1</u> <u>0 0 0</u></blockquote>
 */
public final class BatchedFilterIterator<T> extends AbstractIterator<@Nullable T> {
  private final Iterator<@Nullable T> forward;
  private final int batchSize;
  private final Predicate<@Nullable T> predicate;

  public BatchedFilterIterator(Iterator<@Nullable T> forward, int batchSize, Predicate<@Nullable T> predicate) {
    this.forward = forward;
    this.batchSize = batchSize;
    this.predicate = predicate;
  }

  public Iterator<@Nullable T> forward() {
    return forward;
  }

  public int batchSize() {
    return batchSize;
  }

  public Predicate<@Nullable T> predicate() {
    return predicate;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (BatchedFilterIterator<?>) obj;
    return Objects.equals(this.forward, that.forward) &&
        this.batchSize == that.batchSize &&
        Objects.equals(this.predicate, that.predicate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(forward, batchSize, predicate);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("entityPredicate", forward)
        .append("batchSize", batchSize)
        .append("predicate", predicate)
        .toString();
  }

  @Override
  protected @Nullable T computeNext() {
    for (int i = 0; i < batchSize; i++) {
      if (forward.hasNext()) {
        final var value = forward.next();
        if (predicate.test(value)) {
          return value;
        }
      } else {
        return endOfData();
      }
    }
    return null;
  }

  @Override
  public void forEachRemaining(Consumer<? super @Nullable T> action) {
    while (forward.hasNext()) {
      final T next = forward.next();
      if (predicate.test(next)) action.accept(next);
    }
  }
}
