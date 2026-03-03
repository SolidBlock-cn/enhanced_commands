package pers.solid.ecmd;

import net.minecraft.core.Registry;
import org.jetbrains.annotations.ApiStatus;
import pers.solid.ecmd.api.RegistryBridge;

@ApiStatus.Internal
public interface InitializeContext {
  void validateAndRegister(RegistryBridge<?> registryBridge);

  void registerRegistry(Registry<?> registry);
}
