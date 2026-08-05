package pers.solid.ecmd.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.region.OutlineRegion;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.SphereRegion;
import pers.solid.ecmd.util.enums.OutlineType;

public class RegionDataGeneration implements DynamicRegistryGenerationBridge<Region> {
  private static ResourceKey<Region> of(String value) {
    return ResourceKey.create(Region.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "Regions (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<Region> context) {
    context.add(of("examples/original_sphere"), new SphereRegion(16, new Vec3(0, 64, 0)));
    context.add(of("examples/original_sphere_hollow"), new OutlineRegion(OutlineType.OUTLINE, new SphereRegion(16, new Vec3(0, 64, 0))));
  }
}
