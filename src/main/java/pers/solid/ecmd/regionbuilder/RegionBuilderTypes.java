package pers.solid.ecmd.regionbuilder;

import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.Validate;
import pers.solid.ecmd.EnhancedCommands;

public final class RegionBuilderTypes {
  private RegionBuilderTypes() {}

  public static final RegionBuilderType CUBOID = register(BlockCuboidRegionBuilder::new, "cuboid");
  public static final RegionBuilderType EXTENSION = register(ExtensionCuboidRegionBuilder::new, "extension");
  public static final RegionBuilderType SPHERE = register(SphereRegionBuilder::new, "sphere");

  private static <T extends RegionBuilderType> T register(T regionBuilder, String name) {
    return Registry.register(RegionBuilderType.REGISTRY, new Identifier(EnhancedCommands.MOD_ID, name), regionBuilder);
  }

  public static void init() {
    Validate.notEmpty(RegionBuilderType.REGISTRY.getEntrySet());
  }
}
