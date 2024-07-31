package pers.solid.ecmd.regionselection;

import net.minecraft.registry.Registry;
import org.apache.commons.lang3.Validate;
import pers.solid.ecmd.EnhancedCommands;

public final class RegionSelectionTypes {
  public static final RegionSelectionType CUBOID = register(BlockCuboidRegionSelection::new, "cuboid");
  public static final RegionSelectionType EXTENSION = register(ExtensionCuboidRegionSelection::new, "extension");
  public static final RegionSelectionType SPHERE = register(SphereRegionSelection::new, "sphere");

  private RegionSelectionTypes() {
  }

  private static <T extends RegionSelectionType> T register(T regionBuilder, String name) {
    return Registry.register(RegionSelectionType.REGISTRY, EnhancedCommands.id(name), regionBuilder);
  }

  public static void init() {
    Validate.notEmpty(RegionSelectionType.REGISTRY.getEntrySet());
  }
}
