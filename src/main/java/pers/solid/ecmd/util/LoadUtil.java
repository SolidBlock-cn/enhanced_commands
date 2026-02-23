package pers.solid.ecmd.util;

import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

@SuppressWarnings("deprecation")
public final class LoadUtil {
  private LoadUtil() {
  }

  public static boolean isPosLoaded(LevelReader worldView, int minX, int minZ, int maxX, int maxZ) {
    return worldView.hasChunkAt(minX, minZ) && worldView.hasChunkAt(minX, maxZ) && worldView.hasChunkAt(maxX, minZ) && worldView.hasChunkAt(maxX, maxZ);
  }

  public static boolean isPosLoaded(LevelReader worldView, BoundingBox blockBox) {
    return isPosLoaded(worldView, blockBox.minX(), blockBox.minZ(), blockBox.maxX(), blockBox.maxZ());
  }

  public static boolean isPosLoaded(LevelReader worldView, AABB blockBox) {
    return isPosLoaded(worldView, Mth.floor(blockBox.minX), Mth.floor(blockBox.minZ), Mth.ceil(blockBox.maxX), Mth.ceil(blockBox.maxZ));
  }
}
