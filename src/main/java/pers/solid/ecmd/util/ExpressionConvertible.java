package pers.solid.ecmd.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ExpressionConvertible {
  @Contract(pure = true)
  @NotNull String asString();
}
