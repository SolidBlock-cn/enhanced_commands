package pers.solid.ecmd.util;

import org.jetbrains.annotations.Contract;

public interface ExpressionConvertible {
  @Contract(pure = true)
  String asString();
}
