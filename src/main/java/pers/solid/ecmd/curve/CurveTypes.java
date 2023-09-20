package pers.solid.ecmd.curve;

import com.google.common.base.Preconditions;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.EnhancedCommands;

public final class CurveTypes {
  public static final StraightCurve.Type STRAIGHT = register(StraightCurve.Type.INSTANCE, "straight");
  public static final CircleCurve.Type CIRCLE = register(CircleCurve.Type.INSTANCE, "circle");

  private CurveTypes() {
  }

  public static <T extends CurveType<?>> T register(T curveType, String name) {
    return Registry.register(CurveType.REGISTRY, new Identifier(EnhancedCommands.MOD_ID, name), curveType);
  }

  public static void init() {
    Preconditions.checkState(CurveType.REGISTRY.size() != 0, "CurveType registry is empty");
  }
}
