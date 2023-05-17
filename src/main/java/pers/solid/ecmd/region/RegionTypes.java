package pers.solid.ecmd.region;

import com.google.common.base.Preconditions;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.EnhancedCommands;

public final class RegionTypes {
  public static final RegionType<CuboidRegion> CUBOID = register(CuboidRegion.Type.INSTANCE, "cuboid");
  public static final RegionType<SphereRegion> SPHERE = register(SphereRegion.Type.INSTANCE, "sphere");

  private RegionTypes() {
  }

  private static <T extends Region> RegionType<T> register(RegionType<T> value, String name) {
    return Registry.register(RegionType.REGISTRY, new Identifier(EnhancedCommands.MOD_ID, name), value);
  }

  public static void init() {
    Preconditions.checkState(RegionType.REGISTRY.size() > 0);
  }
}
