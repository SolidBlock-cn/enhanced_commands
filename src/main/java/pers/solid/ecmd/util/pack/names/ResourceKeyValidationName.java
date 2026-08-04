package pers.solid.ecmd.util.pack.names;

import net.minecraft.resources.ResourceKey;

public record ResourceKeyValidationName<T>(ResourceKey<T> key) implements ValidationName {
  @Override
  public String asString() {
    return "{" + key.registry() + " / " + key.location() + "}";
  }
}
