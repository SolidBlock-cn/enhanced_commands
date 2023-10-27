package pers.solid.ecmd.util.iterator;

import java.util.Iterator;
import java.util.function.Consumer;

public final class CatchingIterator<T> implements Iterator<T> {
  private final Iterator<T> forward;
  private final Consumer<RuntimeException> errorConsumer;
  private boolean hasError;

  public CatchingIterator(Iterator<T> forward, Consumer<RuntimeException> errorConsumer) {
    this.forward = forward;
    this.errorConsumer = errorConsumer;
  }

  @Override
  public boolean hasNext() {
    try {
      return !hasError && forward.hasNext();
    } catch (RuntimeException e) {
      errorConsumer.accept(e);
      hasError = true;
      return false;
    }
  }

  @Override
  public T next() {
    try {
      return forward.next();
    } catch (RuntimeException e) {
      errorConsumer.accept(e);
      hasError = true;
      return null;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CatchingIterator<?> that)) return false;

    if (hasError != that.hasError) return false;
    if (!forward.equals(that.forward)) return false;
    return errorConsumer.equals(that.errorConsumer);
  }

  @Override
  public int hashCode() {
    int result = forward.hashCode();
    result = 31 * result + errorConsumer.hashCode();
    result = 31 * result + (hasError ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "CatchingIterator{" +
        "forward=" + forward +
        ", errorConsumer=" + errorConsumer +
        ", hasError=" + hasError +
        '}';
  }
}
