package pers.solid.ecmd.region;

import com.google.common.base.Preconditions;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.EnhancedCommands;

public final class RegionTypes {
  public static final RegionType<CuboidRegion> CUBOID = register(CuboidRegion.Type.CUBOID_TYPE, "cuboid");
  public static final RegionType<SphereRegion> SPHERE = register(SphereRegion.Type.SPHERE_TYPE, "sphere");
  public static final RegionType<IntersectRegion> INTERSECT = register(IntersectRegion.Type.INTERSECT_TYPE, "intersect");
  public static final RegionType<UnionRegion> UNION = register(UnionRegion.Type.UNION_TYPE, "union");
  public static final RegionType<OutlineRegion> OUTLINE = register(OutlineRegion.Type.OUTLINE_TYPE, "outline");
  public static final RegionType<CylinderRegion> CYLINDER = register(CylinderRegion.Type.CYLINDER_TYPE, "cylinder");
  public static final RegionType<HollowCylinderRegion> HOLLOW_CYLINDER = register(HollowCylinderRegion.Type.HOLLOW_CYLINDER_TYPE, "hollow_cylinder");
  public static final RegionType<CuboidOutlineRegion> CUBOID_OUTLINE = register(CuboidOutlineRegion.Type.CUBOID_OUTLINE_TYPE, "cuboid_outline");
  public static final RegionType<CuboidWallRegion> CUBOID_WALL = register(CuboidWallRegion.Type.CUBOID_WALL_TYPE, "cuboid_wall");

  private RegionTypes() {
  }

  private static <T extends Region> RegionType<T> register(RegionType<T> value, String name) {
    return Registry.register(RegionType.REGISTRY, new Identifier(EnhancedCommands.MOD_ID, name), value);
  }

  public static void init() {
    Preconditions.checkState(RegionType.REGISTRY.size() > 0);
  }
}
