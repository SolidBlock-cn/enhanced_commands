package pers.solid.ecmd.data;

import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.region.OutlineRegionProvider;
import pers.solid.ecmd.region.RegionProvider;
import pers.solid.ecmd.region.SphereRegionProvider;
import pers.solid.ecmd.util.enums.OutlineType;

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
    context.add(of("examples/original_sphere"), new SphereRegionProvider(16, EnhancedPosArgument.CURRENT_POS));
    context.add(of("examples/original_sphere_hollow"), new OutlineRegionProvider(OutlineType.OUTLINE, new SphereRegionProvider(16, EnhancedPosArgument.CURRENT_POS)));
  }
}
