package pers.solid.ecmd.fabric;

import net.minecraft.core.Registry;
import pers.solid.ecmd.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;

public class InitializeContextImpl implements InitializeContext {
  @Override
  public void validateAndRegister(RegistryBridge<?> registryBridge) {
    if (registryBridge.isEmpty()) {
      throw new IllegalStateException("Registry " + registryBridge.key().registry() + " is empty!");
    }
  }

  @Override
  public void registerRegistry(Registry<?> registry) {
  }
}
