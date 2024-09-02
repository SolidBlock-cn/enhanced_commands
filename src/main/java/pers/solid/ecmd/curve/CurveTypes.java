package pers.solid.ecmd.curve;

import com.google.common.base.Preconditions;
import net.minecraft.registry.Registry;
import pers.solid.ecmd.EnhancedCommands;

public final class CurveTypes {
  public static final StraightCurve.Type STRAIGHT = register("straight", StraightCurve.Type.INSTANCE);
  public static final CircleCurve.Type CIRCLE = register("circle", CircleCurve.Type.INSTANCE);

  private CurveTypes() {
  }

  public static <T extends CurveType<?>> T register(String name, T curveType) {
    return Registry.register(CurveType.REGISTRY, EnhancedCommands.id(name), curveType);
  }

  public static void init() {
    Preconditions.checkState(CurveType.REGISTRY.size() != 0, "CurveType registry is empty");
  }
}
