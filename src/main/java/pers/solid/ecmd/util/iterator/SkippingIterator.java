package pers.solid.ecmd.util.iterator;

import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public class SkippingIterator<T extends @Nullable Object> implements Iterator<@Nullable T> {
  public final Iterator<T> forward;
  public final int skipTimes;
  public int timesToNext;

  public SkippingIterator(Iterator<T> forward, int skipTimes) {
    this.forward = forward;
    this.skipTimes = skipTimes;
  }

  @Override
  public boolean hasNext() {
    return timesToNext > 0 || forward.hasNext();
  }

  @Override
  public @Nullable T next() {
    if (timesToNext <= 0) {
      timesToNext = skipTimes;
      return forward.next();
    } else {
      timesToNext--;
      return null;
    }
  }
}
