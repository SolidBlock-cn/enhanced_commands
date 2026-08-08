package pers.solid.ecmd.data;

import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.region.RegionProvider;

public class RegionProviderDataGeneration implements DynamicRegistryGenerationBridge<RegionProvider<?>> {
  private static ResourceKey<RegionProvider<?>> of(String value) {
    return ResourceKey.create(RegionProvider.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "Regions (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<RegionProvider<?>> context) {
  }
}
