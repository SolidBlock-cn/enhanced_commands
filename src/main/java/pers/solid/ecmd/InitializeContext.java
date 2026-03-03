package pers.solid.ecmd;

import org.jetbrains.annotations.ApiStatus;
import pers.solid.ecmd.api.RegistryBridge;

@ApiStatus.Internal
public interface InitializeContext {
  void validateAndRegister(RegistryBridge<?> registryBridge);
}
