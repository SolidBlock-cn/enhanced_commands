package pers.solid.ecmd.util.pack.problems;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

public record RecursiveReferenceProblem<T>(ResourceKey<T> resourceKey) implements ValidationProblem {
  @Override
  public Component message() {
    return Component.translatable("enhanced_commands.registry.validation.recursive_reference", resourceKey.registry().toString(), resourceKey.location().toString());
  }
}
