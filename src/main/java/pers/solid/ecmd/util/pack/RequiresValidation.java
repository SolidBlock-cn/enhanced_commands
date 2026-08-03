package pers.solid.ecmd.util.pack;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

public interface RequiresValidation {
  default boolean validate(Context context) {
    boolean result = true;
    for (RequiresValidation member : membersToValidate()) {
      result = result && member.validate(context);
    }
    return true;
  }

  Iterable<? extends RequiresValidation> membersToValidate();

  class Context {
    private final HolderGetter.Provider resolver;
    private final @Unmodifiable Set<ResourceKey<?>> referencedElements;

    public Context(HolderGetter.Provider resolver, Set<ResourceKey<?>> referencedElements) {
      this.resolver = resolver;
      this.referencedElements = referencedElements;
    }

    public Context(HolderGetter.Provider resolver) {
      this.resolver = resolver;
      this.referencedElements = ImmutableSet.of();
    }

    public HolderGetter.Provider resolver() {
      return resolver;
    }

    public boolean isElementReferenced(ResourceKey<?> element) {
      return referencedElements.contains(element);
    }

    public Context withOtherReferencedElement(ResourceKey<?> key) {
      return new Context(resolver, ImmutableSet.<ResourceKey<?>>builder().addAll(referencedElements).add(key).build());
    }
  }
}
