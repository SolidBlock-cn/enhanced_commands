package pers.solid.ecmd.util;

import net.minecraft.util.math.BlockBox;
import net.minecraft.world.WorldView;

@SuppressWarnings("deprecation")
public final class LoadUtil {
  private LoadUtil() {}

  public static boolean isPosLoaded(WorldView worldView, int minX, int minZ, int maxX, int maxZ) {
    return worldView.isPosLoaded(minX, minZ) && worldView.isPosLoaded(minX, maxZ) && worldView.isPosLoaded(maxX, minZ) && worldView.isPosLoaded(maxX, maxZ);
  }

  public static boolean isPosLoaded(WorldView worldView, BlockBox blockBox) {
    return isPosLoaded(worldView, blockBox.getMinX(), blockBox.getMinZ(), blockBox.getMaxX(), blockBox.getMaxZ());
  }
}
