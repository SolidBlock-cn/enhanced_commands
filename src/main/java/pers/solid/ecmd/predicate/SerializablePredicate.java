package pers.solid.ecmd.predicate;

import org.jetbrains.annotations.NotNull;

public interface SerializablePredicate {
  @NotNull String asString();
}
