package pers.solid.ecmd.util.lambda;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;

@FunctionalInterface
public interface ToFloatFunction<T> extends Function<T, Float>, ToDoubleFunction<T> {
  float applyAsFloat(T value);

  @Deprecated
  @Override
  default Float apply(T t) {
    return applyAsFloat(t);
  }

  @Deprecated
  @Override
  default double applyAsDouble(T value) {
    return applyAsFloat(value);
  }
}
