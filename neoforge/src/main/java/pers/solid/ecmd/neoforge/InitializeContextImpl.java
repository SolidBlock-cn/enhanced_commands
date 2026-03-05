package pers.solid.ecmd.neoforge;

import net.minecraft.core.Registry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import pers.solid.ecmd.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.api.neoforge.RegistryBridgeImpl;

public class InitializeContextImpl implements InitializeContext {
  public final IEventBus modEventBus;

  public InitializeContextImpl(IEventBus modEventBus) {
    this.modEventBus = modEventBus;
  }

  @Override
  public void validateAndRegister(RegistryBridge<?> registryBridge) {
    if (!(registryBridge instanceof RegistryBridgeImpl<?> impl)) {
      throw new IllegalStateException(registryBridge + " not instance of neoforge.RegistryBridgeImpl!");
    }
    if (registryBridge.isEmpty()) {
      throw new IllegalStateException("Registry " + registryBridge.key() + " is empty!");
    }
    impl.deferredRegister.register(modEventBus);
  }

  @Override
  public void registerRegistry(Registry<?> registry) {
    modEventBus.addListener(NewRegistryEvent.class, event -> event.register(registry));
  }
}
