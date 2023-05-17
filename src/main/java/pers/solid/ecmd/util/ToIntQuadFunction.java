package pers.solid.ecmd.util;

@FunctionalInterface
public interface ToIntQuadFunction<T, U, V, W> {
  int applyAsInt(T t, U u, V v, W w);
}
