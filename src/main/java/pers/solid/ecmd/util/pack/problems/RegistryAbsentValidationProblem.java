package pers.solid.ecmd.util.pack.problems;

import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

public record RegistryAbsentValidationProblem<T>(ResourceKey<? extends Registry<T>> registryKey) implements ValidationProblem {
  @Override
  public Component message() {
    return Component.translatable("enhanced_commands.registry.validation.registry_absent", registryKey.location().toString());
  }
}
