package pers.solid.ecmd.region;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.util.FunctionsParser;
import pers.solid.ecmd.util.Parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RegionTypes {
  public static final Map<String, RegionType<?>> FUNCTIONS = new LinkedHashMap<>();
  public static final List<Parser<RegionArgument>> PARSERS = Lists.newArrayList(new FunctionsParser<>(FUNCTIONS.keySet(), s -> {
    final RegionType<?> regionType = FUNCTIONS.get(s);
    return regionType == null ? null : regionType.functionParamsParser();
  }, s -> {
    final RegionType<?> regionType = FUNCTIONS.get(s);
    return regionType == null ? null : regionType.tooltip();
  }));

  public static final SingleBlockPosRegion.Type SINGLE = register(SingleBlockPosRegion.Type.INSTANCE, "single");
  public static final CuboidRegion.Type CUBOID = register(CuboidRegion.Type.CUBOID_TYPE, "cuboid");
  public static final SphereRegion.Type SPHERE = register(SphereRegion.Type.SPHERE_TYPE, "sphere");
  public static final IntersectRegion.Type INTERSECT = register(IntersectRegion.Type.INTERSECT_TYPE, "intersect");
  public static final UnionRegion.Type UNION = register(UnionRegion.Type.UNION_TYPE, "union");
  public static final OutlineRegion.Type OUTLINE = register(OutlineRegion.Type.OUTLINE_TYPE, "outline");
  public static final CylinderRegion.Type CYLINDER = register(CylinderRegion.Type.CYLINDER_TYPE, "cylinder");
  public static final HollowCylinderRegion.Type HOLLOW_CYLINDER = register(HollowCylinderRegion.Type.HOLLOW_CYLINDER_TYPE, "hollow_cylinder");
  public static final CuboidOutlineRegion.Type CUBOID_OUTLINE = register(CuboidOutlineRegion.Type.CUBOID_OUTLINE_TYPE, "cuboid_outline");
  public static final CuboidWallRegion.Type CUBOID_WALL = register(CuboidWallRegion.Type.CUBOID_WALL_TYPE, "cuboid_wall");
  public static final OutwardsRegion.Type OUTWARDS = register(OutwardsRegion.Type.INSTANCE, "outwards");
  public static final ActiveRegionType ACTIVE_REGION = register(ActiveRegionType.TYPE, "active_region");

  public static final RegionSelection.Type BUILDER = register(RegionSelection.Type.INSTANCE, "builder");

  private RegionTypes() {
  }

  @SuppressWarnings("unchecked")
  private static <T extends RegionType<?>> T register(T value, String name) {
    final String functionName = value.functionName();
    if (functionName != null) {
      FUNCTIONS.put(functionName, value);
    }
    if (value instanceof Parser<?>) {
      PARSERS.add((Parser<RegionArgument>) value);
    }
    return Registry.register(RegionType.REGISTRY, new Identifier(EnhancedCommands.MOD_ID, name), value);
  }

  public static void init() {
    Preconditions.checkState(RegionType.REGISTRY.size() > 0);
  }
}
