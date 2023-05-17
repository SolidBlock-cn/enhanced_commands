package pers.solid.ecmd.util;

import org.apache.commons.lang3.function.TriFunction;

@FunctionalInterface
public interface TriPredicate<T, U, V> extends TriFunction<T, U, V, Boolean> {
  boolean test(T t, U u, V v);

  @Override
  default Boolean apply(T t, U u, V v) {
    return test(t,u,v);
  }
}
