package pers.solid.ecmd.util.bridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public abstract class AbstractBridgeRange<T extends Comparable<T>> implements BridgeRange<T> {
  public final @Nullable T min;
  public final @Nullable T max;

  protected AbstractBridgeRange(@Nullable T min, @Nullable T max) {
    this.min = min;
    this.max = max;
  }

  @Override
  public @Nullable T getMin() {
    return min;
  }

  @Override
  public @Nullable T getMax() {
    return max;
  }

  @Override
  public boolean test(@NotNull T value) {
    return (min == null || min.compareTo(value) <= 0) && (max == null || max.compareTo(value) >= 0);
  }

  @Override
  public boolean isDummy() {
    return min == null && max == null;
  }

  @Override
  public boolean isExact() {
    return min != null && max != null && min.compareTo(max) == 0;
  }

  @Override
  public Optional<T> getConstantValue() {
    return isExact() ? Optional.ofNullable(min) : Optional.empty();
  }
}
