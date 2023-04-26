package pers.solid.mod.util;

@FunctionalInterface
public interface ToIntQuadFunction<T, U, V, W> {
  int applyAsInt(T t, U u, V v, W w);
}
