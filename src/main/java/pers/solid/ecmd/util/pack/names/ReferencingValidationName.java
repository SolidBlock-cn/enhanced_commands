package pers.solid.ecmd.util.pack.names;

import net.minecraft.resources.ResourceKey;

public record ReferencingValidationName<T>(ValidationName parent, ResourceKeyValidationName<T> resourceKeyName) implements ValidationName {
  public ReferencingValidationName(ValidationName parent, ResourceKey<T> resourceKey) {
    this(parent, new ResourceKeyValidationName<>(resourceKey));
  }

  @Override
  public String asString() {
    return parent.asString() + " referencing " + resourceKeyName.asString();
  }
}
