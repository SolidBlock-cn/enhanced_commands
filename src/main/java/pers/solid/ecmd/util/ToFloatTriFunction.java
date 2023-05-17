package pers.solid.ecmd.util;

import org.apache.commons.lang3.function.TriFunction;

@FunctionalInterface
public interface ToFloatTriFunction<T, U, V> extends TriFunction<T, U, V, Float> {
  float applyAsFloat(T t, U u, V v);

  @Override
  default Float apply(T t, U u, V v) {
    return applyAsFloat(t, u, v);
  }
}
