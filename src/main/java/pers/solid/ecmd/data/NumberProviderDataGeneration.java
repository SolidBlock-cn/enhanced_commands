package pers.solid.ecmd.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.number.EnhancedCommandsNumberProvider;

public class NumberProviderDataGeneration implements DynamicRegistryGenerationBridge<NumberProvider> {
  private static ResourceKey<NumberProvider> of(String value) {
    return ResourceKey.create(EnhancedCommandsNumberProvider.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "Number Providers (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<NumberProvider> context) {
  }
}
