package pers.solid.ecmd.region;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import net.minecraft.registry.Registry;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.util.parse.FunctionsParser;
import pers.solid.ecmd.util.parse.Parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RegionTypes {
  public static final Map<String, RegionType<?>> FUNCTIONS = new LinkedHashMap<>();
  public static final List<Parser<? extends RegionArgument<?>>> PARSERS = Lists.newArrayList(SingleBlockPosRegion.BareParser.INSTANCE, new FunctionsParser<>(FUNCTIONS.keySet(), s -> {
    final RegionType<?> regionType = FUNCTIONS.get(s);
    return regionType == null ? null : regionType.functionParamsParser();
  }, s -> {
    final RegionType<?> regionType = FUNCTIONS.get(s);
    return regionType == null ? null : regionType.tooltip();
  }));

  public static final SingleBlockPosRegion.Type SINGLE = register("single", SingleBlockPosRegion.Type.INSTANCE);
  public static final PreciseCuboidRegion.Type CUBOID = register("cuboid", CuboidRegion.Type.CUBOID_TYPE);
  public static final SphereRegion.Type SPHERE = register("sphere", SphereRegion.Type.SPHERE_TYPE);
  public static final IntersectRegion.Type INTERSECT = register("intersect", IntersectRegion.Type.INTERSECT_TYPE);
  public static final UnionRegion.Type UNION = register("union", UnionRegion.Type.UNION_TYPE);
  public static final OutlineRegion.Type OUTLINE = register("outline", OutlineRegion.Type.OUTLINE_TYPE);
  public static final CylinderRegion.Type CYLINDER = register("cylinder", CylinderRegion.Type.CYLINDER_TYPE);
  public static final HollowCylinderRegion.Type HOLLOW_CYLINDER = register("hollow_cylinder", HollowCylinderRegion.Type.HOLLOW_CYLINDER_TYPE);
  public static final CuboidOutlineRegion.Type CUBOID_OUTLINE = register("cuboid_outline", CuboidOutlineRegion.Type.CUBOID_OUTLINE_TYPE);
  public static final CuboidWallRegion.Type CUBOID_WALL = register("cuboid_wall", CuboidWallRegion.Type.CUBOID_WALL_TYPE);
  public static final OutwardsRegion.Type OUTWARDS = register("outwards", OutwardsRegion.Type.INSTANCE);
  public static final ActiveRegionType ACTIVE_REGION = register("active_region", ActiveRegionType.TYPE);

  public static final RegionSelection.Type BUILDER = register("builder", RegionSelection.Type.INSTANCE);

  private RegionTypes() {
  }

  @SuppressWarnings("unchecked")
  private static <T extends RegionType<?>> T register(String name, T value) {
    final String functionName = value.functionName();
    if (functionName != null) {
      FUNCTIONS.put(functionName, value);
    }
    if (value instanceof Parser<?>) {
      PARSERS.add((Parser<? extends RegionArgument<?>>) value);
    }
    return Registry.register(RegionType.REGISTRY, EnhancedCommands.id(name), value);
  }

  public static void init() {
    Preconditions.checkState(RegionType.REGISTRY.size() > 0);
  }
}
