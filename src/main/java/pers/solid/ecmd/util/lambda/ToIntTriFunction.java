package pers.solid.ecmd.util.lambda;

import org.apache.commons.lang3.function.TriFunction;

@FunctionalInterface
public interface ToIntTriFunction<T, U, V> extends TriFunction<T, U, V, Integer> {
  int applyAsInt(T t, U u, V v);

  @Override
  default Integer apply(T t, U u, V v) {
    return applyAsInt(t, u, v);
  }
}
