package pers.solid.ecmd.util.iterator;

import java.util.Iterator;

public class SkippingIterator<T> implements Iterator<T> {
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
  public T next() {
    if (timesToNext <= 0) {
      timesToNext = skipTimes;
      return forward.next();
    } else {
      timesToNext--;
      return null;
    }
  }
}
